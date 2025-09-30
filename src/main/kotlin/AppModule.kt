import org.jetbrains.skiko.hostOs
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import runtime.AdbStore
import data.remote.adb.Adb
import data.remote.adb.AdbDevicePoller
import data.remote.adb.AdbImpl
import data.remote.adb.Terminal
import data.repository.BookmarkRepository
import viewmodel.FileManagerViewModel
import viewmodel.MainViewModel
import java.io.File

val adbModule = module {
    single { Terminal() }
    single<String>(qualifier = named("adbPath")) {
        File(
            get<AdbStore>().adbHostFile.absolutePath,
            if (hostOs.isWindows) "adb.exe" else "adb"
        ).absolutePath
    }
    single<Adb> { AdbImpl(get(named("adbPath")), get()) }
    single { AdbDevicePoller(get()) }
    single { BookmarkRepository() }
}

val viewModelModule = module {
    viewModel { MainViewModel(get(), get()) }
    viewModel { FileManagerViewModel(get(), get()) }
}