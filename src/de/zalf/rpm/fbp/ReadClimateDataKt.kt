package de.zalf.rpm.fbp

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.jpaulmorrison.fbp.core.engine.*
import com.opencsv.bean.CsvToBeanBuilder
import org.pcollections.PMap

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@ComponentDescription("Replace references to 'functions' in coll representation of a JSON object")
@InPorts(InPort(value = "IN", description = "A CSV String", type = String::class), InPort(value = "OPTS", description = "Options", type = Map::class))
@OutPorts(OutPort(value = "OUT", description = "Map representation of JSON object", type = PMap::class), OutPort(value = "ERROR", description = "Error message", type = String::class, optional = true))
class ReadClimateDataKt : Component() {

    private lateinit var inPort: InputPort
    private lateinit var optsPort: InputPort
    private lateinit var outPort: OutputPort
    private lateinit var errorPort: OutputPort

    private val objectMapper = CsvMapper()

    private var delimiter = ","
    private var noOfHeaderLines = 1
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var renameMap: Map<String, Any>? = null

    inner class Data {
        var data: MutableMap<String, Double> = mutableMapOf(
                "tmin" to -9999.0,
                "tavg" to -9999.0,
                "tmax" to -9999.0,
                "precip" to -1.0,
                "globrad" to -1.0,
                "wind" to -1.0,
                "sunhours" to -1.0,
                "cloudamount" to -1.0,
                "relhumid" to -1.0,
                "airpress" to -1.0,
                "vaporpress" to -1.0,
                "co2" to -1.0,
                "o3" to -1.0,
                "et0" to -1.0)
        var date: Date? = null
        var tmin: Double by data
        var tavg: Double by data
        var tmax: Double by data
        var precip: Double by data
        var globrad: Double by data
        var wind: Double by data
        var sunhours: Double by data
        var cloudamount: Double by data
        var relhumid: Double by data
        var airpress: Double by data
        var vaporpress: Double by data
        var co2: Double by data
        var o3: Double by data
        var et0: Double by data
    }

    private fun readClimateData(csvStr : String){

        val lines = csvStr.lines()
        val header = lines.first().split(delimiter.toRegex()).dropLastWhile { it.isEmpty() }
        var trans = HashMap<String, (Double) -> Double>()
        val header2 = if (renameMap != null) {
            header.map { name ->
                if (renameMap!!.contains(name)){
                    val o = renameMap!![name]
                    when (o) {
                        is String -> o
                        is List<*> -> if (o.size > 0) {
                            if (o.size > 2 && o[0] is String && o[1] is String && o[2] is Number) {
                                val value = o[2] as Double
                                trans[o[0] as String] = when (o[1]){
                                    "+" -> { v -> v + value }
                                    "-" -> { v -> v - value }
                                    "*" -> { v -> v * value }
                                    "/" -> { v -> v / value }
                                    else -> { v -> v }
                                }
                            }
                            o[0] as String
                        } else
                            name
                        else -> name
                    }
                } else
                    name
            }
        } else
            header

        val needed = setOf(
                "day", "month", "year", "de-date", "iso-date",
                "tmin", "tavg", "tmax",
                "precip", "globrad", "relhumid", "wind",
                "vaporpress", "airpress", "cloudamount", "sunhours",
                "co2", "o3", "et0")
        var extractMap = HashMap<String, Int>()
        header2.forEachIndexed { index, name -> if (needed.contains(name)) extractMap[name] = index }

        val isoDf = SimpleDateFormat("yyyy-MM-dd")
        val deDf = SimpleDateFormat("dd.MM.yyyy")

        //var dates = ArrayList<Date>()
        //var data = HashMap<String, List<Double>>()
        var data = ArrayList<Data>()
        for (line in lines.drop(noOfHeaderLines)) {
            var dmy = mutableListOf(0, 0, 0)
            var date : Date? = null
            val splitLine = line.split(delimiter.toRegex())
            val lineData = Data()
            for ((name, index) in extractMap) {
                when (name) {
                    "day" -> dmy[0] = splitLine[index].toInt()
                    "month" -> dmy[1] = splitLine[index].toInt()
                    "year" -> dmy[2] = splitLine[index].toInt()
                    "de-date" -> date = deDf.parse(splitLine[index])
                    "iso-date" -> date = isoDf.parse(splitLine[index])
                    "tmin", "tavg", "tmax",
                    "precip", "globrad", "relhumid", "wind",
                    "vaporpress", "airpress", "cloudamount", "sunhours",
                    "co2", "o3", "et0" -> {
                        val v = splitLine[index].toDouble()
                        lineData.data[name] = if(trans.containsKey(name)) trans[name]?.invoke(v) ?: v else v
                    }
                }
            }
            lineData.date = when {
                date != null -> date
                dmy.all { it > 0 } -> GregorianCalendar(dmy[2], dmy[1], dmy[0]).time
                else -> null
            }

            if (lineData.tavg < -9998.0 && lineData.tmin > -9999.0 && lineData.tmax > -9999.0)
                lineData.tavg = (lineData.tmin + lineData.tmax) / 2.0


            if (lineData.date != null)
                data.plus(lineData)
        }

        try {
            csvStr.lineSequence()


            val br = BufferedReader(StringReader(csvStr))
            br.mark(1000)
            val headerLine = br.readLine()
            val header = headerLine.split(delimiter.toRegex()).dropLastWhile({ it.isEmpty() })
            br.reset()

            val csv2bean = CsvToBeanBuilder<Data>(br)
                    .withSeparator(delimiter.first())
                    .withSkipLines(noOfHeaderLines)
                    .build()
            val data = csv2bean.parse()

        } catch (e: IOException) {
            //continue
        }



    }

    override fun execute() {

        if (!optsPort.isClosed) {
            val op = optsPort.receive() ?: return
            val df = SimpleDateFormat("yyyy-MM-dd")
            val options = op.content as Map<String, Any>
            if (options.containsKey("csv-separator"))
                delimiter = options["csv-separator"] as String
            if (options.containsKey("no-of-climate-file-header-lines"))
                noOfHeaderLines = options["no-of-climate-file-header-lines"] as Int
            if (options.containsKey("start-date")) {
                try {
                    startDate = df.parse(options["start-date"] as String)
                } catch (e: ParseException) {}
            }
            if (options.containsKey("end-date")) {
                try {
                    endDate = df.parse(options["end-date"] as String)
                } catch (e: ParseException) {}
            }
            if (options.containsKey("header-to-acd-names"))
                renameMap = options["header-to-acd-names"] as Map<String, Any>

            drop(op)
            optsPort.close()
        }

        //CsvSchema schema = CsvSchema.builder().






        while (true) {
            val ip = inPort.receive() ?: break
            val csvStr = ip.content as String
            drop(ip)
            try {
                val br = BufferedReader(StringReader(csvStr))
                br.mark(1000)
                val headerLine = br.readLine()
                val header = headerLine.split(delimiter.toRegex()).dropLastWhile({ it.isEmpty() })
                br.reset()

                val csv2bean = CsvToBeanBuilder<Data>(br)
                        .withSeparator(delimiter.first())
                        .withSkipLines(noOfHeaderLines)
                        .build()
                val data = csv2bean.parse()

            } catch (e: IOException) {
                continue
            }


            val bootstrapSchema = CsvSchema.emptySchema().withHeader()


            //Object updatedMap = findAndReplaceReferences(m, m);
            //if(PMap.class.isAssignableFrom(updatedMap.getClass()))
            //    m = (PMap<String, Object>)updatedMap;
            drop(ip)
            //Packet out = create(m);
            //outPort.send(out);
        }
    }

    override fun openPorts() {
        inPort = openInput("IN")
        optsPort = openInput("OPTS")
        outPort = openOutput("OUT")
        errorPort = openOutput("ERROR")
    }

}
