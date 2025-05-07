package com.study.android.flow

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val flow = flow {
            emit("A")
            emit("B")
        }
        lifecycleScope.launch {
            flow.collect { println(it) }
        }

        val sharedFlow = MutableSharedFlow<String>()
        lifecycleScope.launch {
            sharedFlow.collect { println("Subscriber 1: $it") }
        }
        lifecycleScope.launch {
            sharedFlow.emit("First")
        }

        val sharedFlowReplay = MutableSharedFlow<Int>(
            replay = 1 // Хранит последнее значение для новых подписчиков
        )
        lifecycleScope.launch {
            sharedFlowReplay.emit(1)
        }
        lifecycleScope.launch {
            sharedFlowReplay.collect { println("Subscriber 1: $it") }
        }

        val sharedFlowBuffer = MutableSharedFlow<Int>(
            extraBufferCapacity = 2 // Дополнительно хранит до 2 значений в буфере
        )
        lifecycleScope.launch {
            sharedFlowBuffer.emit(1)
            sharedFlowBuffer.emit(2)
            sharedFlowBuffer.emit(3)
        }
        lifecycleScope.launch {
            sharedFlowBuffer.collect { println("Subscriber 1: $it") }
        }


        val stateFlow = MutableStateFlow(0)
        stateFlow.value = 1

        lifecycleScope.launch {
            stateFlow.collect { println("Value: $it") } // получит 1 сразу
        }

        val channel = Channel<Int>()
        lifecycleScope.launch { channel.send(1) }
        lifecycleScope.launch { println(channel.receive()) } // Выведет 1


        val flow1 = flowOf(1, 2)
        val flow2 = flowOf("A", "B")
        lifecycleScope.launch {
            flow1.combine(flow2) { i, s -> "$i$s" }
                .collect { println(it) } // Выведет: 1A, 1B, 2B
        }

        var counter = 0

        runBlocking {
            val jobs = List(100) {
                launch {
                    repeat(1000) {
                        counter++  // НЕбезопасно!
                    }
                }
            }
            jobs.forEach { it.join() }
            println("Counter: $counter") // Ожидаем 100_000, но может быть меньше!
        }

        runBlocking {
            val jobs = List(100) {
                launch {
                    repeat(1000) {
                        mutex.withLock {
                            counter++
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
            println("Counter: $counter") // Теперь всегда 100_000
        }

        val nameFlow = flowOf("Alice", "Bob", "Charlie")
        val ageFlow = flowOf(20, 25, 30)

        lifecycleScope.launch {
            nameFlow.zip(ageFlow) { name, age ->
                "$name is $age years old"
            }.collect { info ->
                Log.d("ZipExample", info)
            }
        }


    }

    val mutex = Mutex()

    suspend fun criticalSection() {
        mutex.withLock {
            // Код, который должен быть выполнен эксклюзивно
        }
    }
}