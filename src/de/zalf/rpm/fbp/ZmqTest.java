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
        defineCreateEnvSubnetTest();
    }

    protected void defineCreateEnvSubnetTest(){
        component("map_->_json",de.zalf.rpm.fbp.CreateJsonStringFromMap.class);
        component("to_console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("sim_->_map",de.zalf.rpm.fbp.CreateMapFromJsonFile.class);
        component("crop_->_map",de.zalf.rpm.fbp.CreateMapFromJsonFile.class);
        component("site_->_map",de.zalf.rpm.fbp.CreateMapFromJsonFile.class);
        component("replace_sim_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("replace__crop_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("replace__site_refs",de.zalf.rpm.fbp.ReplaceReferences.class);
        component("template__->_map",de.zalf.rpm.fbp.CreateMapFromJsonFile.class);
        component("set__debugMode",de.zalf.rpm.fbp.SetValueInMap.class);
        component("get_debug?",de.zalf.rpm.fbp.GetValueFromMap.class);
        component("set_simParams",de.zalf.rpm.fbp.SetValueInMap.class);
        component("get_events",de.zalf.rpm.fbp.GetValueFromMap.class);
        component("get_cropRotation",de.zalf.rpm.fbp.GetValueFromMap.class);
        component("set_cropRotation",de.zalf.rpm.fbp.SetValueInMap.class);
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
        component("json_->_map",de.zalf.rpm.fbp.CreateMapFromJsonString.class);
        component("map_->_json",de.zalf.rpm.fbp.CreateJsonStringFromMap.class);
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
        component("JSON_string_to_Map",de.zalf.rpm.fbp.CreateMapFromJsonString.class);
        component("write_to__console",com.jpaulmorrison.fbp.core.components.misc.WriteToConsole.class);
        component("set_values",de.zalf.rpm.fbp.SetValueInMap.class);
        component("repeat",de.zalf.rpm.fbp.RepeatableStream.class);
        component("Map_to__JSON_string",de.zalf.rpm.fbp.CreateJsonStringFromMap.class);
        component("pass_and_trigger",de.zalf.rpm.fbp.PassThroughWithTrigger.class);
        component("set_values_2",de.zalf.rpm.fbp.SetValueInMap.class);
        component("decompose_into_words_2",com.jpaulmorrison.fbp.core.components.text.DeCompose.class);
        component("repeat_2",de.zalf.rpm.fbp.RepeatableStream.class);
        component("pass_and_trigger_2",de.zalf.rpm.fbp.PassThroughWithTrigger.class);
        component("read_file",com.jpaulmorrison.fbp.core.components.io.ReadFile.class);
        component("concat_strings",de.zalf.rpm.fbp.ConcatString.class);
        connect(component("set_values_2"), port("OUT"), component("Map_to__JSON_string"), port("IN"));
        connect(component("pass_and_trigger_2"), port("TRIGGER"), component("repeat_2"), port("REPEAT"));
        initialize("climate.csv-options, header-to-acd-names, globrad, 0", component("set_values_2"), port("PATH"));
        connect(component("pass_and_trigger_2"), port("OUT"), component("set_values_2"), port("MAP"));
        initialize("sim.json", component("read_file"), port("SOURCE"));
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
        component("JSON String -> Map", de.zalf.rpm.fbp.CreateMapFromJsonString.class);
        component("Map -> JSON String", de.zalf.rpm.fbp.CreateJsonStringFromMap.class);
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
