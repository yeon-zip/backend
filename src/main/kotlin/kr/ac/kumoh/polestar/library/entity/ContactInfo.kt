package kr.ac.kumoh.polestar.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class ContactInfo(
    homepageUrl: String? = null,
    tel: String? = null
) {
    @Column(name = "homepage_url", length = 1000)
    var homepageUrl: String? = homepageUrl
        protected set

    @Column(name = "tel", length = 100)
    var tel: String? = tel
        protected set
}
