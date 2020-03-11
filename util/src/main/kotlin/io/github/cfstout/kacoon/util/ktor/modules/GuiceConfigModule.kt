package com.classpass.bespoke.proxy.modules

import com.google.inject.AbstractModule

class GuiceConfigModule : AbstractModule() {
    override fun configure() {
        binder().requireAtInjectOnConstructors()
        binder().requireExactBindingAnnotations()
        binder().requireExplicitBindings()
    }
}
