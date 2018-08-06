package de.zalf.rpm.fbp;    // change this if you want

import com.jpaulmorrison.fbp.core.engine.Network;

public class ZmqTest extends Network {
    String description = " ";

    protected void define() {
       //defineZmqTest();
       //defineCountTest();
       //defineJsonToMapAndBackTest();
       defineSetValuesInMapTest();
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
