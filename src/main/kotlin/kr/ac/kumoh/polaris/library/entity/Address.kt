package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class Address(
    province: String? = null,
    city: String? = null,
    detail: String? = null
) {
    @Column(name = "address_province")
    var province: String? = province
        protected set

    @Column(name = "address_city")
    var city: String? = city
        protected set

    @Column(name = "address_detail")
    var detail: String? = detail
        protected set

    override fun toString(): String {
        return listOf(province, city, detail)
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .joinToString(" ")
    }
}
