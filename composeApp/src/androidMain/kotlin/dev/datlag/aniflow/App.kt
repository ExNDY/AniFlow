package dev.datlag.aniflow

import android.content.Context
import androidx.multidex.MultiDexApplication
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import dev.datlag.aniflow.module.NetworkModule
import dev.datlag.aniflow.other.StateSaver
import dev.datlag.sekret.NativeLoader
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindSingleton
import org.kodein.di.instance

class App : MultiDexApplication(), DIAware {

    override val di: DI = DI {
        bindSingleton<Context> {
            applicationContext
        }

        import(NetworkModule.di)
    }

    @OptIn(DelicateCoilApi::class)
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
        StateSaver.sekretLibraryLoaded = NativeLoader.loadLibrary("sekret")

        val imageLoader by di.instance<ImageLoader>()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

}