package com.presisco.lazystorm.spout

import com.presisco.lazystorm.bolt.Constants
import com.presisco.toolbox.time.StopWatch
import org.apache.storm.spout.SpoutOutputCollector
import org.apache.storm.task.TopologyContext
import org.apache.storm.topology.OutputFieldsDeclarer
import org.apache.storm.topology.base.BaseRichSpout
import org.apache.storm.tuple.Fields
import org.apache.storm.tuple.Values

abstract class TimedSpout : BaseRichSpout() {
    private var intervalSec: Long = 60

    @Transient
    private lateinit var collector: SpoutOutputCollector

    protected fun <T> Map<String, *>.byType(key: String): T = if (this.containsKey(key)) this[key] as T else throw IllegalStateException("$key not defined in config")

    protected fun Map<String, *>.getInt(key: String) = this.byType<Number>(key).toInt()

    protected fun Map<String, *>.getLong(key: String) = this.byType<Number>(key).toLong()

    protected fun Map<String, *>.getString(key: String) = this.byType<String>(key)

    protected fun Map<String, *>.getBoolean(key: String) = this.byType<Boolean>(key)

    protected fun <K, V> Map<String, *>.getMap(key: String) = this.byType<Map<K, V>>(key)

    protected fun Map<String, *>.getHashMap(key: String) = this.byType<HashMap<String, Any?>>(key)

    protected fun <E> Map<String, *>.getList(key: String) = this.byType<List<E>>(key)

    protected fun <E> Map<String, *>.getArrayList(key: String) = this.byType<ArrayList<E>>(key)

    protected fun Map<String, *>.getListOfMap(key: String) = this[key] as List<Map<String, *>>

    protected fun <K, V> Map<String, V>.mapKeyToHashMap(keyMap: (key: String) -> K): HashMap<K, V> {
        val hashMap = hashMapOf<K, V>()
        this.forEach { key, value -> hashMap[keyMap(key)] = value }
        return hashMap
    }

    protected fun <Old, New> Map<String, Old>.mapValueToHashMap(valueMap: (value: Old) -> New): HashMap<String, New> {
        val hashMap = hashMapOf<String, New>()
        this.forEach { key, value -> hashMap[key] = valueMap(value) }
        return hashMap
    }

    abstract fun producer(): HashMap<String, *>?

    fun setIntervalSec(sec: Long): TimedSpout {
        intervalSec = sec
        return this
    }

    override fun nextTuple() {
        val stopWatch = StopWatch()
        stopWatch.start()
        val data = producer()
        data?.let {
            collector.emit(Values(it))
        }
        stopWatch.stop()
        val duration = stopWatch.durationFromStart()
        if (duration < intervalSec) {
            Thread.sleep(intervalSec - duration)
        }
    }

    override fun open(config: MutableMap<*, *>, context: TopologyContext, collector: SpoutOutputCollector) {
        this.collector = collector
    }

    override fun declareOutputFields(declarer: OutputFieldsDeclarer) {
        declarer.declare(
                Fields(Constants.DATA_FIELD_NAME)
        )
    }
}