package com.mokostudio.baselog.core.user

enum class BaseballTeam(
    val id: String,
    val displayName: String
) {
    DoosanBears(id = "doosan_bears", displayName = "Doosan Bears"),
    HanwhaEagles(id = "hanwha_eagles", displayName = "Hanwha Eagles"),
    KiaTigers(id = "kia_tigers", displayName = "KIA Tigers"),
    KiwoomHeroes(id = "kiwoom_heroes", displayName = "Kiwoom Heroes"),
    KtWiz(id = "kt_wiz", displayName = "KT Wiz"),
    LgTwins(id = "lg_twins", displayName = "LG Twins"),
    LotteGiants(id = "lotte_giants", displayName = "Lotte Giants"),
    NcDinos(id = "nc_dinos", displayName = "NC Dinos"),
    SamsungLions(id = "samsung_lions", displayName = "Samsung Lions"),
    SsgLanders(id = "ssg_landers", displayName = "SSG Landers");

    companion object {
        fun fromId(id: String): BaseballTeam? = entries.firstOrNull { it.id == id }

        fun fromDisplayName(displayName: String): BaseballTeam? =
            entries.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }
    }
}
