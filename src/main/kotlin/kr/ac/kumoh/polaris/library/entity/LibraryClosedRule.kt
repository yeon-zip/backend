package kr.ac.kumoh.polaris.library.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "library_closed_rule")
class LibraryClosedRule(
    library: Library,
    ruleType: ClosedRuleType,
    weekday: Int? = null,
    nthWeek: Int? = null,
    monthDay: Int? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_id", nullable = false)
    var library: Library = library
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 30)
    var ruleType: ClosedRuleType = ruleType
        protected set

    @Column(name = "weekday")
    var weekday: Int? = weekday
        protected set

    @Column(name = "nth_week")
    var nthWeek: Int? = nthWeek
        protected set

    @Column(name = "month_day")
    var monthDay: Int? = monthDay
        protected set

    fun changeRule(
        ruleType: ClosedRuleType,
        weekday: Int?,
        nthWeek: Int?,
        monthDay: Int?
    ) {
        this.ruleType = ruleType
        this.weekday = weekday
        this.nthWeek = nthWeek
        this.monthDay = monthDay
    }
}
