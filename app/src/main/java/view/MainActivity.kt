package view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import model.dataObjects.Group
import model.database.AppDatabase
import model.database.AppRepository
import view.navigation.AppNavHost
import view.ui.theme.DiceCompanionTheme

class MainActivity : ComponentActivity() {

    private val database by lazy {
        AppDatabase.getInstance(context = applicationContext)
    }
    private val repository: AppRepository by lazy {
        //AppRepository.create(scope = CoroutineScope(Dispatchers.IO), database = database)
        AppRepository(scope = CoroutineScope(Dispatchers.IO), database = database)
    }


    private fun ensureRootGroup(): Long = runBlocking(Dispatchers.IO) {
        //val root = database.groupDao().getGroup(fullpath = "/")
        val root = database.groupDao().getRootGroup()
        if (root != null) {
            return@runBlocking root.id
        } else {
            return@runBlocking repository.upsertGroup(

                Group(
                    name = "",
                    parentId = null,
                    //path = null,
                    templateName = ""
                )

            )
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DiceCompanionTheme {
                val rootId = ensureRootGroup()

                Surface(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                ) {
                    AppNavHost(
                        rootId = rootId,
                        repository = repository
                    )
                }

            }
        }
    }



    @Preview
    @Composable
    fun MainActivity_Preview(){
        DiceCompanionTheme {
            // A surface container using the 'background' color from the theme

            Surface {

            }
        }
    }

}


