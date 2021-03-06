package com.addressbook.model

import com.addressbook.dto.OrganizationDto
import org.apache.ignite.cache.query.annotations.QuerySqlField
import java.sql.Timestamp
import java.util.*

class Organization constructor(id: String?, name: String?, addr: Address?, type: OrganizationType?, lastUpdated: Timestamp?) {

    @QuerySqlField(index = true)
    var id: String? = null

    @QuerySqlField(index = true)
    var name: String? = null

    @QuerySqlField(index = true)
    var addr: Address? = null

    @QuerySqlField(index = true)
    var type: OrganizationType? = null

    @QuerySqlField(index = true)
    var lastUpdated: Timestamp? = null

    constructor(organizationDto: OrganizationDto) : this(organizationDto.id, null, null, null, null)
    constructor(name: String) : this(UUID.randomUUID().toString(), name, null, null, null)
    constructor(id: String, name: String) : this(id, name, null, null, null)
    constructor(name: String, addr: Address, type: OrganizationType, lastUpdated: Timestamp) : this(UUID.randomUUID().toString(), name, addr, type, lastUpdated)

    init {
        this.id = id
        this.name = name
        this.addr = addr
        this.type = type
        this.lastUpdated = lastUpdated
    }
}
