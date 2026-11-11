package com.iagothz.mergetd.ui

import com.iagothz.mergetd.logic.Mission
import com.iagothz.mergetd.logic.MissionKind

/** The mission sentence in the current language: "Mate 40 inimigos com Ignis". */
fun Mission.text(): String {
    val what = when (kind) {
        MissionKind.KILLS_WITH -> Skin.fullName(type!!)
        MissionKind.REACH_WAVE -> S[map!!.title]
        MissionKind.PLAY_MODE -> S[mode!!.title]
        else -> null
    }
    return when {
        !template.contains("%d") -> S[template]
        what == null -> S.f(template, target)
        else -> S.f(template, target, what)
    }
}
