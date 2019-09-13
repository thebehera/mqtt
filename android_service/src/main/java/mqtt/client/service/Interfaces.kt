package mqtt.client.service

import android.app.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class CoroutineService : Service(), CoroutineScope {
    protected val job: Job = Job()
    protected val dispatcher = Dispatchers.Main
    override val coroutineContext: CoroutineContext = job + dispatcher
}
