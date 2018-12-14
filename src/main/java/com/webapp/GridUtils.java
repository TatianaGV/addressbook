package com.webapp;

import com.webapp.dto.*;
import com.webapp.model.*;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.core.io.DefaultResourceLoader;
import javax.cache.Cache;
import java.lang.reflect.Constructor;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class GridUtils {

    private static Ignite ignite ;

    static void startClient(){
        if(ignite != null) return;
        try{
            final DefaultResourceLoader loader = new DefaultResourceLoader();
            ignite = Ignition.start(loader.getResource("classpath:example-default.xml").getFile().getAbsolutePath());
            ignite.active(true);
            for(Map.Entry<String, Class> cache : UniversalFieldsDescriptor.getCacheClasses().entrySet()){
                CacheConfiguration cfg = new CacheConfiguration<>();
                cfg.setCacheMode(CacheMode.PARTITIONED);
                cfg.setName(cache.getKey());
                cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
                cfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC);
                cfg.setIndexedTypes(String.class, cache.getValue());
                try (IgniteCache createdCache = ignite.getOrCreateCache(cfg)) {
                    if (ignite.cluster().forDataNodes(createdCache.getName()).nodes().isEmpty()) {
                        System.out.println();
                        System.out.println(">>> Please start at least 1 remote cache node.");
                        System.out.println();
                    }
                }
            }
            MenuCreator.initMenu();
        }catch (Exception e){
            System.out.println("Exception during Ignite Client startup: ");
            e.printStackTrace();
        }
    }

    public static OrganizationDto createOrUpdateOrganization(OrganizationDto organizationDto){
        IgniteCache<String, Organization> cachePerson = ignite.getOrCreateCache(UniversalFieldsDescriptor.ORGANIZATION_CACHE);
        Organization organization = cachePerson.get(organizationDto.getId());
        if(organization == null) organization = new Organization(organizationDto);
        organization.setType(OrganizationType.values()[Integer.valueOf(organizationDto.getType())]);
        organization.setLastUpdated(new Timestamp(System.currentTimeMillis()));
        organization.setName(organizationDto.getName());
        organization.setAddr(new Address(organizationDto.getStreet(), Integer.valueOf(organizationDto.getZip())));
        cachePerson.put(organization.getId(), organization);
        return organizationDto;
    }

    public static PersonDto createOrUpdatePerson(PersonDto personDto){
        IgniteCache<String, Person> cachePerson = ignite.getOrCreateCache(UniversalFieldsDescriptor.PERSON_CACHE);
        Person person = cachePerson.get(personDto.getId());
        if(person == null) person = new Person(personDto);
        person.setFirstName(personDto.getFirstName());
        person.setLastName(personDto.getLastName());
        person.setOrgId(personDto.getOrgId());
        person.setSalary(personDto.getSalary() != null ? Double.valueOf(personDto.getSalary()) : 0);
        person.setResume(personDto.getResume());
        cachePerson.put(person.getId(), person);
        return personDto;
    }

    public static ContactDto createOrUpdateContact(ContactDto contactDto){
        IgniteCache<String, Contact> cachePerson = ignite.getOrCreateCache(UniversalFieldsDescriptor.CONTACT_CACHE);
        Contact contact = cachePerson.get(contactDto.getId());
        if(contact == null) contact = new Contact();
        contact.setData(contactDto.getData());
        contact.setDescription(contactDto.getDescription());
        contact.setPersonId(contactDto.getPersonId());
        contact.setType(ContactType.values()[Integer.valueOf(contactDto.getType())]);
        cachePerson.put(contact.getContactId(), contact);
        return contactDto;
    }

    public static void clearMenus(){
        ignite.getOrCreateCache(UniversalFieldsDescriptor.MENU_CACHE).clear();
    }

    public static MenuEntryDto createOrUpdateMenuEntry(MenuEntryDto menuEntryDto, MenuEntryDto parentEntryDto){
        IgniteCache<String, MenuEntry> cachePerson = ignite.getOrCreateCache(UniversalFieldsDescriptor.MENU_CACHE);
        MenuEntry menuEntry;
        if(menuEntryDto.getId() != null) menuEntry = cachePerson.get(menuEntryDto.getId());
        else menuEntry = new MenuEntry();
        menuEntry.setName(menuEntryDto.getName());
        menuEntry.setUrl(menuEntryDto.getUrl());
        if(parentEntryDto != null) menuEntry.setParentId(parentEntryDto.getId());
        menuEntryDto.setId(menuEntry.getId());
        cachePerson.put(menuEntry.getId(), menuEntry);
        return menuEntryDto;
    }

    public static List<MenuEntryDto> readNextLevel(String url){
        IgniteCache<String, MenuEntry> cache = ignite.getOrCreateCache(UniversalFieldsDescriptor.MENU_CACHE);
        MenuEntry menuEntry = cache.query(new SqlQuery<String, MenuEntry>(MenuEntry.class, "url = ?").setArgs(url)).getAll().get(0).getValue();
        SqlQuery<String, MenuEntry> sql = new SqlQuery<>(MenuEntry.class, "parentId = ?");
        List<MenuEntryDto> menuEntryDtos = new ArrayList<>();
        try (QueryCursor<Cache.Entry<String, MenuEntry>> cursor = cache.query(sql.setArgs(menuEntry.getId()))) {
            for (Cache.Entry<String, MenuEntry> e : cursor)
                menuEntryDtos.add(new MenuEntryDto(e.getValue()));
        }catch (Exception e){
            e.printStackTrace();
        }
        return menuEntryDtos;
    }

    public static List<Breadcrumb> readBreadcrumbs(String url){
        IgniteCache<String, MenuEntry> cache = ignite.getOrCreateCache(UniversalFieldsDescriptor.MENU_CACHE);
        MenuEntry original = cache.query(new SqlQuery<String, MenuEntry>(MenuEntry.class, "url = ?").setArgs(url)).getAll().get(0).getValue();;
        MenuEntry menuEntry = original;
        List<Cache.Entry<String, MenuEntry>> menuEntries;
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        if(menuEntry.getParentId() == null) return breadcrumbs;
        SqlQuery<String, MenuEntry> sql = new SqlQuery<>(MenuEntry.class, "id = ?");
        while (true){
            menuEntries = cache.query(sql.setArgs(menuEntry.getParentId())).getAll();
            if(!menuEntries.isEmpty()){
                menuEntry = menuEntries.get(0).getValue();
                breadcrumbs.add(0, new Breadcrumb(menuEntry.getName(), menuEntry.getUrl()));
            }else break;
        }
        breadcrumbs.add(new Breadcrumb(original.getName(), original.getUrl()));
        return breadcrumbs;
    }

    private static StringBuilder getQuerySql(List<FilterDto> filterDto){
        StringBuilder baseSql = new StringBuilder(" ");
        if(filterDto.size() != 0){
            for(FilterDto filter : filterDto){
                String type = filter.getType();
                String addSql = "";
                if(type.equals("NumberFilter")){
                    Integer query;
                    try {
                        query = Integer.valueOf(filter.getValue());
                    }catch (NumberFormatException e){
                        System.out.println("Invalid filter argument: " + filter.getValue());
                        continue;
                    }
                    addSql = filter.getName() + getComparator(filter) + query;
                }
                if(type.equals("TextFilter")){
                    addSql = filter.getName() + " like '%" + filter.getValue().replaceAll("'","''") + "%'";
                }
                if(type.equals("DateFilter")){
                    addSql = filter.getName() + getComparator(filter) + "'" + filter.getValue() + "'";
                }
                if(filterDto.indexOf(filter) == 0) baseSql.append(" where ");
                baseSql.append(addSql);
                if(filterDto.indexOf(filter) != (filterDto.size() - 1)) baseSql.append(" and ");
            }
        }
        return baseSql;
    }

    public static List<?> selectCachePage(int page, int pageSize, String sortName, String sortOrder, List<FilterDto> filterDto, String cacheName){
        IgniteCache cache = ignite.getOrCreateCache(cacheName);
        List cacheDtoArrayList = new ArrayList<>();
        SqlQuery sql = new SqlQuery(UniversalFieldsDescriptor.getCacheClass(cacheName), getQuerySql(filterDto)
                                                                                        .append(" order by ")
                                                                                        .append(sortName).append(" ")
                                                                                        .append(sortOrder)
                                                                                        .append(" limit ? offset ?")
                                                                                        .toString());
        try (QueryCursor<Cache.Entry> cursor = cache.query(sql.setArgs(pageSize, (page - 1) * pageSize))) {
            Constructor dtoConstructor = UniversalFieldsDescriptor.getDtoClass(cacheName).getConstructor(UniversalFieldsDescriptor.getCacheClass(cacheName));
            for (Cache.Entry e : cursor)
                cacheDtoArrayList.add(dtoConstructor.newInstance(e.getValue()));
        }catch (Exception e){
            e.printStackTrace();
        }
        return cacheDtoArrayList;
    }

    public static List<ContactDto> getContactsByPersonId(String id){
        IgniteCache<String, Contact> cache = ignite.getOrCreateCache(UniversalFieldsDescriptor.CONTACT_CACHE);
        List<ContactDto> cacheDtoArrayList = new ArrayList<>();
        SqlQuery sql = new SqlQuery(Contact.class, "personId = ?");
        try (QueryCursor<Cache.Entry> cursor = cache.query(sql.setArgs(id))) {
            for (Cache.Entry<String, Contact> e : cursor)
                cacheDtoArrayList.add(new ContactDto(e.getValue()));
        }catch (Exception e){
            e.printStackTrace();
        }
        return cacheDtoArrayList;

    }

    private static String getComparator(FilterDto filterDto){
        if(filterDto.getComparator() == null || filterDto.getComparator().equals(""))
            return " = ";
        else return " " + filterDto.getComparator() + " ";
    }

    public static Integer getTotalDataSize(String cacheName, List<FilterDto> filterDto){
        if(filterDto.size() == 0){
            IgniteCache cache = ignite.getOrCreateCache(cacheName);
            return cache.size(CachePeekMode.ALL);
        }
        IgniteCache cache = ignite.getOrCreateCache(cacheName);
        SqlQuery sql = new SqlQuery(UniversalFieldsDescriptor.getCacheClass(cacheName), getQuerySql(filterDto).toString());
        try (QueryCursor<Cache.Entry> cursor = cache.query(sql)) {
            return cursor.getAll().size();
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    static void stopClient(){
        if(ignite != null){
            try{
                ignite.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
