package com.yzddmr6.prismspace.prism.model

sealed class Space {
    abstract val displayName: String

    object Main : Space() {
        override val displayName: String = "主空间"
    }

    data class ManagedProfile(val userId: Int, val name: String = "双开空间") : Space() {
        override val displayName: String = name
    }

    data class Experimental(val userId: Int, val name: String) : Space() {
        override val displayName: String = name
    }
}
