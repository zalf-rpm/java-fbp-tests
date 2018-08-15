package de.zalf.rpm.fbp;    // change this if you want

import com.jpaulmorrison.fbp.core.engine.Network;

public class ZmqTest extends Network {
    String description = " ";

    protected void define() {
        //defineZmqTest();
        //defineCountTest();
        //defineJsonToMapAndBackTest();
        //defineSetValuesInMapTest();
        //defineReplaceReferencesComponentTest();
        //defineCreateEnvSubnetTest();
        //defineGetValueTest();
        //defineSetValueTest();
        //defineMappingTest();
        //defineCreateEnvTest();
        defineClimateCSV2JSONTest();
    }

    protected void defineClimateCSV2JSONTest(){
        component("read_file_into_lines",com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("read_sim.json",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("get_csv_options",de.zalf.rpm.fbp.GetValueFromColl.class);
        component("split_line",de.zalf.rpm.fbp.SplitString.class);
        component("get_separator",de.zalf.rpm.fbp.GetValueFromColl.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("toString()",de.zalf.rpm.fbp.ToString.class);
        component("select_header_lines",de.zalf.rpm.fbp.SelectNthIP.class);
        component("get_#_of_headerlines",de.zalf.rpm.fbp.GetValueFromColl.class);
        component("select_header_line",de.zalf.rpm.fbp.SelectNthIP.class);
        component("split_data_into_streams",de.zalf.rpm.fbp.SplitList.class);
        component("to_list_of_dates",de.zalf.rpm.fbp.ToList.class);
        component("to_list_of_tmins",de.zalf.rpm.fbp.ToList.class);
        component("to_list__of_tavgs",de.zalf.rpm.fbp.ToList.class);
        component("drop_tmins",de.zalf.rpm.fbp.Sink.class);
        component("drop_tavgs",de.zalf.rpm.fbp.Sink.class);
        component("drop_sim",de.zalf.rpm.fbp.Sink.class);
        component("drop_options",de.zalf.rpm.fbp.Sink.class);
        initialize("climate.csv", component("read_file_into_lines"), port("SOURCE"));
        initialize("sim.json", component("read_sim.json"), port("IN"));
        connect(component("read_sim.json"), port("OUT"), component("get_csv_options"), port("IN"));
        initialize("climate.csv-options", component("get_csv_options"), port("PATH"));
        connect(component("get_csv_options"), port("OUT"), component("get_separator"), port("IN"));
        initialize("csv-separator", component("get_separator"), port("PATH"));
        connect(component("get_separator"), port("OUT"), component("split_line"), port("AT"));
        connect(component("read_file_into_lines"), port("OUT"), component("split_line"), port("IN"));
        connect(component("toString()"), port("OUT"), component("to_console"), port("IN"));
        connect(component("split_line"), port("OUT"), component("select_header_lines"), port("IN"));
        connect(component("get_#_of_headerlines"), port("OUT"), component("select_header_lines"), port("COUNT"));
        initialize("no-of-climate-file-header-lines", component("get_#_of_headerlines"), port("PATH"));
        connect(component("select_header_lines"), port("ACC"), component("select_header_line"), port("IN"));
        connect(component("select_header_lines"), port("REJ"), component("split_data_into_streams"), port("IN"));
        initialize("0,1,2", component("split_data_into_streams"), port("SEL"));
        connect(component("split_data_into_streams"), port("OUT[0]"), component("to_list_of_dates"), port("IN"));
        connect(component("split_data_into_streams"), port("OUT[1]"), component("to_list_of_tmins"), port("IN"));
        connect(component("split_data_into_streams"), port("OUT[2]"), component("to_list__of_tavgs"), port("IN"));
        connect(component("to_list_of_dates"), port("OUT"), component("toString()"), port("IN"));
        connect(component("to_list_of_tmins"), port("OUT"), component("drop_tmins"), port("IN"));
        connect(component("to_list__of_tavgs"), port("OUT"), component("drop_tavgs"), port("IN"));
        connect(component("get_separator"), port("PASS"), component("get_#_of_headerlines"), port("IN"));
        connect(component("get_csv_options"), port("PASS"), component("drop_sim"), port("IN"));
        connect(component("get_#_of_headerlines"), port("PASS"), component("drop_options"), port("IN"));
    }


    protected void defineCreateEnvTest(){
        component("read_mapping",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("List_to_IPs_1",de.zalf.rpm.fbp.ListToIPs.class);
        component("split_map",de.zalf.rpm.fbp.SplitMap.class);
        component("List_to_IPs_2",de.zalf.rpm.fbp.ListToIPs.class);
        component("split_list",de.zalf.rpm.fbp.SplitList.class);
        component("to_console_3",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("to_string_2",de.zalf.rpm.fbp.ToJsonString.class);
        component("replace_references",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("get_value",de.zalf.rpm.fbp.GetValueFromColl.class);
        component("ZMQ_send",de.zalf.rpm.fbp.ZeroMQOutProxy.class);
        component("read_files",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("set_value",de.zalf.rpm.fbp.SetValueInPColl.class);
        component("read_template",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("flatten_paths",de.zalf.rpm.fbp.JoinSubstreams.class);
        component("flatten_values",de.zalf.rpm.fbp.JoinSubstreams.class);
        initialize("env-template.json", component("read_template"), port("IN"));
        connect(component("read_mapping"), port("OUT"), component("List_to_IPs_1"), port("IN"));
        connect(component("read_template"), port("OUT"), component("set_value"), port("COLL"));
        connect(component("List_to_IPs_1"), port("OUT"), component("split_map"), port("IN"));
        connect(component("split_map"), port("OUT[1]"), component("List_to_IPs_2"), port("IN"));
        connect(component("set_value"), port("OUT"), component("to_string_2"), port("IN"));
        connect(component("List_to_IPs_2"), port("OUT"), component("split_list"), port("IN"));
        initialize("0", component("List_to_IPs_2"), port("SSL"));
        connect(component("split_list"), port("OUT[0]"), component("get_value"), port("PATH"), 2);
        connect(component("split_list"), port("OUT[1]"), component("flatten_paths"), port("IN"));
        connect(component("flatten_paths"), port("OUT"), component("set_value"), port("PATH"));
        connect(component("get_value"), port("OUT"), component("flatten_values"), port("IN"));
        connect(component("flatten_values"), port("OUT"), component("set_value"), port("VALUE"));
        initialize("0,1", component("split_list"), port("SEL"));
        initialize("get-from, mapping", component("split_map"), port("SEL"));
        initialize("mapping.json", component("read_mapping"), port("IN"));
        connect(component("read_files"), port("OUT"), component("replace_references"), port("IN"));
        connect(component("replace_references"), port("OUT"), component("get_value"), port("IN"));
        connect(component("to_string_2"), port("OUT"), component("ZMQ_send"), port("IN"));
        connect(component("ZMQ_send"), port("OUT"), component("to_console_3"), port("IN"));
        connect(component("split_map"), port("OUT[0]"), component("read_files"), port("IN"));
        initialize("tcp://10.10.26.34:6666", component("ZMQ_send"), port("ADDRESS"));
    }

    protected void defineMappingTest(){
        component("read_mapping",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("List_to_IPs_1",de.zalf.rpm.fbp.ListToIPs.class);
        component("split_map",de.zalf.rpm.fbp.SplitMap.class);
        component("List_to_IPs_2",de.zalf.rpm.fbp.ListToIPs.class);
        component("split_list",de.zalf.rpm.fbp.SplitList.class);
        component("to_console_3",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("to_string_2",de.zalf.rpm.fbp.ToJsonString.class);
        component("get_value",de.zalf.rpm.fbp.GetValueFromColl.class);
        component("read_files",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("set_value",de.zalf.rpm.fbp.SetValueInPColl.class);
        component("read_template",de.zalf.rpm.fbp.JsonFileToPColl.class);
        component("flatten_paths",de.zalf.rpm.fbp.JoinSubstreams.class);
        component("flatten_values",de.zalf.rpm.fbp.JoinSubstreams.class);
        initialize("env-template.json", component("read_template"), port("IN"));
        connect(component("read_mapping"), port("OUT"), component("List_to_IPs_1"), port("IN"));
        connect(component("read_template"), port("OUT"), component("set_value"), port("COLL"));
        connect(component("List_to_IPs_1"), port("OUT"), component("split_map"), port("IN"));
        connect(component("split_map"), port("OUT[1]"), component("List_to_IPs_2"), port("IN"));
        connect(component("set_value"), port("OUT"), component("to_string_2"), port("IN"));
        connect(component("List_to_IPs_2"), port("OUT"), component("split_list"), port("IN"));
        initialize("0", component("List_to_IPs_2"), port("SSL"));
        connect(component("split_list"), port("OUT[0]"), component("get_value"), port("PATH"), 2);
        connect(component("split_list"), port("OUT[1]"), component("flatten_paths"), port("IN"));
        connect(component("flatten_paths"), port("OUT"), component("set_value"), port("PATH"));
        connect(component("get_value"), port("OUT"), component("flatten_values"), port("IN"));
        connect(component("flatten_values"), port("OUT"), component("set_value"), port("VALUE"));
        initialize("0,1", component("split_list"), port("SEL"));
        initialize("get-from, mapping", component("split_map"), port("SEL"));
        initialize("mapping.json", component("read_mapping"), port("IN"));
        connect(component("to_string_2"), port("OUT"), component("to_console_3"), port("IN"));
        connect(component("split_map"), port("OUT[0]"), component("read_files"), port("IN"));
        connect(component("read_files"), port("OUT"), component("get_value"), port("IN"));
    }

    protected void defineSetValueTest(){
        component("read_sim", JsonFileToPColl.class);
        component("read_paths", JsonStringToPColl.class);
        component("set_value", SetValueInPColl.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("read_values", JsonStringToPColl.class);
        component("split_values",de.zalf.rpm.fbp.ListToIPs.class);
        component("split_paths",de.zalf.rpm.fbp.ListToIPs.class);
        component("coll->string", ToJsonString.class);
        connect(component("read_sim"), port("OUT"), component("set_value"), port("COLL"));
        initialize("sim-simple.json", component("read_sim"), port("IN"));
        initialize("[[\"climate.csv-options\", \"start-date\"], [\"crop.json\"]]", component("read_paths"), port("IN"));
        connect(component("read_paths"), port("OUT"), component("split_paths"), port("IN"));
        connect(component("split_paths"), port("OUT"), component("set_value"), port("PATH"));
        connect(component("split_values"), port("OUT"), component("set_value"), port("VALUE"));
        connect(component("read_values"), port("OUT"), component("split_values"), port("IN"));
        initialize("[1, 2, 3]", component("read_values"), port("IN"));
        initialize("0", component("split_values"), port("SSL"));
        connect(component("set_value"), port("OUT"), component("coll->string"), port("IN"));
        connect(component("coll->string"), port("OUT"), component("to_console"), port("IN"));
    }

    protected void defineGetValueTest(){
        component("read_sim", JsonFileToPColl.class);
        component("string->coll", JsonStringToPColl.class);
        component("get_value", GetValueFromColl.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        connect(component("get_value"), port("OUT"), component("to_console"), port("IN"));
        connect(component("read_sim"), port("OUT"), component("get_value"), port("IN"));
        connect(component("string->coll"), port("OUT"), component("get_value"), port("PATH"));
        initialize("sim-simple.json", component("read_sim"), port("IN"));
        initialize("[\"climate.csv-options\", \"start-date\"]", component("string->coll"), port("IN"));
    }

    protected void defineCreateEnvSubnetTest(){
        component("map_->_json", ToJsonString.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("sim_->_map", JsonFileToPColl.class);
        component("crop_->_map", JsonFileToPColl.class);
        component("site_->_map", JsonFileToPColl.class);
        component("replace_sim_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("replace__crop_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("replace__site_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("template__->_map", JsonFileToPColl.class);
        component("set__debugMode", SetValueInPColl.class);
        component("get_debug?", GetValueFromColl.class);
        component("set_simParams", SetValueInPColl.class);
        component("get_events", GetValueFromColl.class);
        component("get_cropRotation", GetValueFromColl.class);
        component("set_cropRotation", SetValueInPColl.class);
        component("drop_site",de.zalf.rpm.fbp.Sink.class);
        initialize("sim.json", component("sim_->_map"), port("IN"));
        initialize("crop.json", component("crop_->_map"), port("IN"));
        initialize("site.json", component("site_->_map"), port("IN"));
        connect(component("map_->_json"), port("OUT"), component("to_console"), port("IN"));
        connect(component("sim_->_map"), port("OUT"), component("replace_sim_refs"), port("IN"));
        connect(component("crop_->_map"), port("OUT"), component("replace__crop_refs"), port("IN"));
        connect(component("site_->_map"), port("OUT"), component("replace__site_refs"), port("IN"));
        initialize("env-template.json", component("template__->_map"), port("IN"));
        connect(component("template__->_map"), port("OUT"), component("set__debugMode"), port("MAP"));
        initialize("debugMode", component("set__debugMode"), port("PATH"));
        connect(component("replace_sim_refs"), port("OUT"), component("get_debug?"), port("IN"));
        initialize("debug?", component("get_debug?"), port("PATH"));
        connect(component("get_debug?"), port("VALUE"), component("set__debugMode"), port("VALUE"));
        initialize("params, simulationParameters", component("set_simParams"), port("PATH"));
        connect(component("set__debugMode"), port("OUT"), component("set_simParams"), port("MAP"));
        initialize("output, events", component("get_events"), port("PATH"));
        connect(component("get_debug?"), port("PASS"), component("get_events"), port("IN"));
        connect(component("get_events"), port("VALUE"), component("set_simParams"), port("VALUE"));
        connect(component("replace__crop_refs"), port("OUT"), component("get_cropRotation"), port("IN"));
        connect(component("set_simParams"), port("OUT"), component("set_cropRotation"), port("MAP"));
        connect(component("get_cropRotation"), port("VALUE"), component("set_cropRotation"), port("VALUE"));
        initialize("cropRotation", component("set_cropRotation"), port("PATH"));
        initialize("cropRotation", component("get_cropRotation"), port("PATH"));
        connect(component("set_cropRotation"), port("OUT"), component("map_->_json"), port("IN"));
        connect(component("replace__site_refs"), port("OUT"), component("drop_site"), port("IN"));
    }

    protected void defineReplaceReferencesComponentTest() {
        component("replace_ref_functions",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("read_file",com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("concat_file",de.zalf.rpm.fbp.ConcatString.class);
        component("json_->_map", JsonStringToPColl.class);
        component("map_->_json", ToJsonString.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        connect(component("read_file"), port("OUT"), component("concat_file"), port("IN"));
        connect(component("concat_file"), port("OUT"), component("json_->_map"), port("IN"));
        initialize("crop.json", component("read_file"), port("SOURCE"));
        connect(component("json_->_map"), port("OUT"), component("replace_ref_functions"), port("IN"));
        connect(component("replace_ref_functions"), port("OUT"), component("map_->_json"), port("IN"));
        connect(component("map_->_json"), port("OUT"), component("to_console"), port("IN"));
    }

    protected void defineSetValuesInMapTest() {
        component("decompose_into_words",com.jpaulmorrison.fbp.core.components.text.DeCompose.class);
        component("JSON_string_to_Map", JsonStringToPColl.class);
        component("write_to__console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("set_values", SetValueInPColl.class);
        component("repeat",de.zalf.rpm.fbp.RepeatableStream.class);
        component("Map_to__JSON_string", ToJsonString.class);
        component("pass_and_trigger",de.zalf.rpm.fbp.PassThroughWithTrigger.class);
        component("set_values_2", SetValueInPColl.class);
        component("decompose_into_words_2",com.jpaulmorrison.fbp.core.components.text.DeCompose.class);
        component("repeat_2",de.zalf.rpm.fbp.RepeatableStream.class);
        component("pass_and_trigger_2",de.zalf.rpm.fbp.PassThroughWithTrigger.class);
        component("read_file",com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("concat_strings",de.zalf.rpm.fbp.ConcatString.class);
        connect(component("set_values_2"), port("OUT"), component("Map_to__JSON_string"), port("IN"));
        connect(component("pass_and_trigger_2"), port("TRIGGER"), component("repeat_2"), port("REPEAT"));
        initialize("climate.csv-options, header-to-acd-names, globrad, 0", component("set_values_2"), port("PATH"));
        connect(component("pass_and_trigger_2"), port("OUT"), component("set_values_2"), port("MAP"));
        initialize("sim-simple.json", component("read_file"), port("SOURCE"));
        connect(component("read_file"), port("OUT"), component("concat_strings"), port("IN"));
        initialize("climate.csv-options, start-date", component("set_values"), port("PATH"));
        initialize("hello my very dear world ", component("decompose_into_words"), port("IN"));
        connect(component("concat_strings"), port("OUT"), component("JSON_string_to_Map"), port("IN"));
        connect(component("Map_to__JSON_string"), port("OUT"), component("write_to__console"), port("IN"));
        connect(component("decompose_into_words"), port("OUT"), component("repeat"), port("IN"));
        connect(component("repeat"), port("OUT"), component("set_values"), port("VALUE"));
        connect(component("JSON_string_to_Map"), port("OUT"), component("pass_and_trigger"), port("IN"));
        connect(component("pass_and_trigger"), port("OUT"), component("set_values"), port("MAP"));
        connect(component("pass_and_trigger"), port("TRIGGER"), component("repeat"), port("REPEAT"));
        initialize("what the heck ", component("decompose_into_words_2"), port("IN"));
        connect(component("decompose_into_words_2"), port("OUT"), component("repeat_2"), port("IN"));
        connect(component("repeat_2"), port("OUT"), component("set_values_2"), port("VALUE"));
        connect(component("set_values"), port("OUT"), component("pass_and_trigger_2"), port("IN"));
    }

    protected void defineJsonToMapAndBackTest() {
        component("concat file", de.zalf.rpm.fbp.ConcatString.class);
        component("read file", com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("print to console", com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("JSON String -> Map", JsonStringToPColl.class);
        component("Map -> JSON String", ToJsonString.class);
        initialize("sim.json", component("read file"), port("SOURCE"));
        connect(component("read file"), port("OUT"), component("concat file"), port("IN"));
        connect(component("concat file"), port("OUT"), component("JSON String -> Map"), port("IN"));
        connect(component("JSON String -> Map"), port("OUT"), component("Map -> JSON String"), port("IN"));
        connect(component("Map -> JSON String"), port("OUT"), component("print to console"), port("IN"));
        initialize("\n", component("concat file"), port("SEP"));
    }

    protected void defineCountTest() {
        component("concat_file", de.zalf.rpm.fbp.ConcatString.class);
        component("read_file", com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("print_to_console_", com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("print_to_console_count", com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("counter", com.jpaulmorrison.fbp.core.components.misc.Counter.class);
        initialize("sim.json", component("read_file"), port("SOURCE"));
        connect(component("read_file"), port("OUT"), component("counter"), port("IN"));
        connect(component("counter"), port("OUT"), component("concat_file"), port("IN"));
        connect(component("counter"), port("COUNT"), component("print_to_console_count"), port("IN"));
        connect(component("concat_file"), port("OUT"), component("print_to_console_"), port("IN"));
        initialize("\n", component("concat_file"), port("SEP"));
    }

    protected void defineZmqTest() {
        component("concat_file", de.zalf.rpm.fbp.ConcatString.class);
        component("read_file", com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("send_to_server",de.zalf.rpm.fbp.ZeroMQOutProxy.class);
        component("print_to_console", com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        initialize("tcp://localhost:6666", component("send_to_server"), port("ZMQ-ADDRESS"));
        initialize("sim.json", component("read_file"), port("SOURCE"));
        connect(component("read_file"), port("OUT"), component("concat_file"), port("IN"));
        connect(component("concat_file"), port("OUT"), component("send_to_server"), port("IN"));
        connect(component("concat_file"), port("OUT"), component("print_to_console"), port("IN"));
        initialize("\n", component("concat_file"), port("SEP"));
        connect(component("send_to_server"), port("OUT"), component("print_to_console"), port("IN"));
    }

    public static void main(String[] argv) throws Exception {
        new ZmqTest().go();
    }
}
