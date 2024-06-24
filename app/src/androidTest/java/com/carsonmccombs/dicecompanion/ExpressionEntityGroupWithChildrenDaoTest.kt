package com.carsonmccombs.dicecompanion

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ExpressionEntityGroupWithChildrenDaoTest {
    @Test
    fun fake() = assert(true)
/*
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    lateinit var appDatabase: AppDatabase
    lateinit var dao: ExpressionGroupWithChildrenDao

    @Before
    fun setup(){
        appDatabase = Room.inMemoryDatabaseBuilder(
            context = ApplicationProvider.getApplicationContext(),
            klass = AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = appDatabase.expressionGroupWithChildrenDao()
    }

    @Test
    fun something() = runBlocking{

        val rootExpressionGroupWithChildren = ExpressionGroupWithChildren(
            expressionGroup = ExpressionGroup(
                name = "",
                path = ""
            ),
            childExpressionGroups = emptyList(),
            childExpressions = listOf(
                Expression(
                    name = "ExpressionA",
                    text = "1"
                )
            )
        )
        dao.InsertExpressionGroupWithChildren(rootExpressionGroupWithChildren)
        try{
            val latch = CountDownLatch(1)
            val job = async(Dispatchers.IO) {
                dao.GetExpressionGroupWithFullPath("").collect{
                    println("Expression Groups:\n${it.expressionGroup}")
                    println("Expression Group Children:${it.childExpressionGroups.joinToString("\n")}")
                    println("Expression Group Children:${it.childExpressions.joinToString("\n")}")

                    latch.countDown()
                }
            }
            latch.await()
            job.cancelAndJoin()
            assert(true)
        } catch(err: Error) {
            assert(false)
        }

    }

    @After
    fun close(){
        appDatabase.close()
    }*/
}