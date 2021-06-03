package ru.kuchanov.huaweiandgoogleservices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.agconnect.remoteconfig.AGConnectConfig
import com.huawei.hms.aaid.HmsInstanceId
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.kuchanov.huaweiandgoogleservices.R
import ru.kuchanov.huaweiandgoogleservices.analytics.Analytics
import ru.kuchanov.huaweiandgoogleservices.analytics.EventOpenMainScreen
import ru.kuchanov.huaweiandgoogleservices.analytics.EventOpenMapScreen
import ru.kuchanov.huaweiandgoogleservices.location.LocationGateway
import timber.log.Timber

class MainFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    private val analytics: Analytics by inject()
    private val locationGateway: LocationGateway by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendEventButton.setOnClickListener { analytics.send(EventOpenMainScreen()) }

        locationButton.setOnClickListener {
            locationGateway
                .requestLastLocation()
                .subscribeBy(
                    onSuccess = {
                        Snackbar.make(root, "Location: $it", Snackbar.LENGTH_SHORT).show()
                    },
                    onError = {
                        it.printStackTrace()
                        Snackbar.make(root, "Location error: ${it.message}", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                )
                .addTo(compositeDisposable)
        }

        locationUpdatesButton.setOnClickListener {
            locationGateway
                .requestLocationUpdates()
                .subscribeBy(
                    onNext = {
                        Snackbar
                            .make(root, "Location: $it", Snackbar.LENGTH_SHORT)
                            .show()
                    },
                    onError = {
                        it.printStackTrace()
                        Snackbar
                            .make(root, "Location error: ${it.message}", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                )
                .addTo(compositeDisposable)
        }

        openMapButton.setOnClickListener {
            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.container, MapFragment(), MapFragment::class.java.simpleName)
                ?.addToBackStack(MapFragment::class.java.simpleName)
                ?.commit()

            analytics.send(EventOpenMapScreen())
        }

        val testParam = "test_param"
        val config = AGConnectConfig.getInstance()
        config.clearAll()
        config.applyDefault(mapOf(testParam to "test_value_default"))
        Timber.d("config testParam: ${config.getValueAsString(testParam)}")
        remoteConfigUpdateButton.setOnClickListener {
            config.fetch()
                .addOnSuccessListener {
                    Timber.d("config testParam: ${it.getValueAsString(testParam)}")
                    Timber.d("config testParam: ${config.getValueAsString(testParam)}")
                    config.apply(it)
                    Timber.d("config testParam: ${config.getValueAsString(testParam)}")
                }
                .addOnFailureListener {
                    Timber.e(it)
                }
        }

        getPushTokenButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appId =
                        AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                    val token = HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                    if (token.isEmpty().not()) {
                        Timber.i("obtainToken() token: $token")
                    }
                } catch (e: Exception) {
                    Timber.e("obtainToken() failed, $e")
                }
            }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}