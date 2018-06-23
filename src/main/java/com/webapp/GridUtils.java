package com.webapp;

import com.webapp.dto.FilterDto;
import com.webapp.dto.OrganizationDto;
import com.webapp.model.Address;
import com.webapp.model.Organization;
import com.webapp.model.OrganizationType;
import com.webapp.model.Person;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
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

@SuppressWarnings("unchecked")
public class GridUtils {

    private static Ignite ignite ;

    static void startClient(){
        if(ignite != null) return;
        try{
            final DefaultResourceLoader loader = new DefaultResourceLoader();
            ignite = Ignition.start(loader.getResource("classpath:example-default.xml").getFile().getAbsolutePath());

            for(Map.Entry<String, Class> cache : UniversalFieldsDescriptor.getCacheClasses().entrySet()){
                CacheConfiguration cfg = new CacheConfiguration<>();

                cfg.setCacheMode(CacheMode.PARTITIONED);
                cfg.setName(cache.getKey());
                cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
                cfg.setIndexedTypes(String.class, cache.getValue());

                try (IgniteCache createdCache = ignite.getOrCreateCache(cfg)) {
                    if (ignite.cluster().forDataNodes(createdCache.getName()).nodes().isEmpty()) {
                        System.out.println();
                        System.out.println(">>> This example requires remote cache node nodes to be started.");
                        System.out.println(">>> Please start at least 1 remote cache node.");
                        System.out.println(">>> Refer to example's javadoc for details on configuration.");
                        System.out.println();
                    }
                }
            }

            insertTestData();
        }catch (Exception e){
            System.out.println("Exception during Ignite Client startup: ");
            e.printStackTrace();
        }

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
        System.out.println(baseSql.toString());
        return baseSql;
    }

    public static List<OrganizationDto> selectCachePage(int page, int pageSize, String sortName, String sortOrder, List<FilterDto> filterDto, String cacheName){
        IgniteCache cache = ignite.getOrCreateCache(cacheName);
        ArrayList cacheDtoArrayList = new ArrayList<>();
        SqlQuery sql = new SqlQuery(UniversalFieldsDescriptor.getCacheClass(cacheName), getQuerySql(filterDto).append(" order by " + sortName + " " + sortOrder + " limit ? offset ?").toString());
        try (QueryCursor<Cache.Entry> cursor = cache.query(sql.setArgs(pageSize, (page - 1) * pageSize))) {
            Constructor dtoConstructor = UniversalFieldsDescriptor.getDtoClass(cacheName).getConstructor(UniversalFieldsDescriptor.getCacheClass(cacheName));
            for (Cache.Entry e : cursor)
                cacheDtoArrayList.add(dtoConstructor.newInstance(e.getValue()));
        }catch (Exception e){
            e.printStackTrace();
        }
        return cacheDtoArrayList;
    }

    private static String getComparator(FilterDto filterDto){
        if(filterDto.getComparator() == null || filterDto.getComparator().equals(""))
            return " = ";
        else
            return " " + filterDto.getComparator() + " ";
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

    private static void insertTestData(){
        System.out.println("Inserting test data");
        IgniteCache<String, Organization> cacheOrganization = ignite.getOrCreateCache("com.webapp.model.Organization");
        IgniteCache<String, Person> cachePerson = ignite.getOrCreateCache("com.webapp.model.Person");
        for(int i = 0; i < 50; i++){
            Organization org = new Organization(
                    "Microsoft " + Math.random(),
                    new Address("1096 Eddy Street, San Francisco, CA " + Math.random(), (int)(Math.random()*100000)),
                    OrganizationType.PRIVATE,
                    new Timestamp(System.currentTimeMillis()));
            cacheOrganization.put(org.getId(),org);

            Person person = new Person(org, "Nikita " + Math.random(), "Loshakov " + Math.random(), 3500.90, "Senior Java Developer");
            cachePerson.put(person.getId(),person);

            Person person1 = new Person(org, "Nikita " + Math.random(), "Loshakov " + Math.random(), 3500.90, "Senior Java Developer");
            cachePerson.put(person1.getId(),person1);
        }
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
