package de.zalf.rpm.fbp;

import com.fasterxml.jackson.datatype.pcollections.PCollectionsModule;
import com.jpaulmorrison.fbp.core.engine.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.util.Pair;
import org.pcollections.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ComponentDescription("Replace references to 'functions' in origIn representation of a JSON object")
@InPort(value = "IN", description = "Map representation of a JSON object", type = PMap.class)
@OutPorts({
        @OutPort(value = "OUT", description = "Map representation of JSON object", type = PMap.class),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class ReplaceReferences  extends Component {

    InputPort inPort;
    OutputPort outPort;
    OutputPort errorPort;

    interface PatternFunc { Object call(Map root, List j); }
    HashMap<String, PatternFunc> supportedPatterns = new HashMap<>();

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new PCollectionsModule());

    public ReplaceReferences(){
        buildSupportedPatterns();
    }

    Object findAndReplaceReferences(Map root, Object j)
    {
        boolean success = true;

        if(List.class.isAssignableFrom(j.getClass()) && ((List)j).size() > 0) {

            List jl = (List)j;
            PVector<Object> arr = Empty.vector();

            //is array a reference function?
            if(jl.get(0) instanceof String) {

                if(supportedPatterns.containsKey((String)jl.get(0))) {
                    PatternFunc p = supportedPatterns.get((String)jl.get(0));

                    //check for nested function invocations in the arguments
                    PVector<Object> funcArgs = Empty.vector();
                    for(Object i : jl.subList(1, jl.size())) {
                        Object r = findAndReplaceReferences(root, i);
                        funcArgs = funcArgs.plus(r);
                    }

                    //invoke function
                    Object jaes = p.call(root, funcArgs);
                    //if successful try to recurse into result for functions in result
                    if(jaes != null) {
                        Object r = findAndReplaceReferences(root, jaes);
                        return r;
                    }
                    else
                        return jl;
                }
            }

            //array was no reference function, threat it as normal array
            for (Object jv : jl) {
                Object r = findAndReplaceReferences(root, jv);
                arr = arr.plus(r);
            }
            return arr;
        }
        else if(Map.class.isAssignableFrom(j.getClass())) {
            PMap<String, Object> obj = Empty.map();

            for(Map.Entry<String, Object> p : ((Map<String, Object>)j).entrySet())
            {
                Object r = findAndReplaceReferences(root, p.getValue());
                obj = obj.plus(p.getKey(), r);
            }

            return obj;
        }

        return j;
    }

    String replaceEnvVars(String path) {
        //"replace ${ENV_VAR} in path"
        String start_token = "${";
        String end_token = "}";
        int start_pos = path.indexOf(start_token);
        while(start_pos > -1)
        {
            int end_pos = path.indexOf(end_token, start_pos + 1);
            if(end_pos > -1)
            {
                int name_start = start_pos + 2;
                String env_var_name = path.substring(name_start, end_pos);
                String env_var_content = System.getenv(env_var_name);
                if(env_var_content.length() > 0)
                {
                    path = path.replace(path.substring(start_pos, end_pos + 1), env_var_content);
                    start_pos = path.indexOf(start_token);
                }
                else
                    start_pos = path.indexOf(start_token, end_pos + 1);
            }
            else
                break;
        }

        return path;
    }

    HashMap<Pair<String, String>, Object> refCache;
    void buildSupportedPatterns() {

        PatternFunc ref = (Map root, List args) ->
        {
            if(refCache == null)
                refCache = new HashMap<>();

            if(args.size() == 2
               && args.get(0) instanceof String
               && args.get(1) instanceof String)
            {
                String key1 = (String)args.get(0);
                String key2 = (String)args.get(1);

                if(refCache.containsKey(new Pair(key1, key2)))
                    return refCache.get(new Pair(key1, key2));

                try {
                    Object res = findAndReplaceReferences(root, ((Map)root.get(key1)).get(key2));
                    if(res != null)
                        refCache.put(new Pair(key1, key2), res);
                    return res;
                } catch(ClassCastException cce){

                }
            }
            if(errorPort.isConnected())
                errorPort.send(create("Couldn't resolve reference: [ref, " + args + "]!"));
            return null;
        };

        PatternFunc fromFile = (Map root, List args) ->
        {
            if(args.size() == 1
               && args.get(0) instanceof String)
            {
                String basePath = (String)root.get("include-file-base-path");
                if(basePath == null)
                    basePath = ".";
                else
                    basePath = replaceEnvVars(basePath);
                String pathToFile = (String)args.get(0);
                pathToFile = replaceEnvVars(pathToFile);

                if(!new File(pathToFile).isAbsolute())
                    pathToFile = basePath + "/" + pathToFile;

                try {
                    Map m = objectMapper.readValue(new File(pathToFile), HashPMap.class);
                    return m;
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
            if(errorPort.isConnected())
                errorPort.send(create("Couldn't include file with function: " + args + "!"));
            return null;
        };

        /*
        auto humus2corg = [](const Json&, const Json& j) -> EResult<Json>
        {
            if(j.array_items().size() == 2
                    && j[1].is_number())
            {
                auto ecorg = Soil::humusClass2corg(j[1].int_value());
                if(ecorg.success())
                    return{ecorg.result};
                else
                    return{j, ecorg.errors};
            }
            return{j, string("Couldn't convert humus level to corg: ") + j.dump() + "!"};
        };

        auto bdc2rd = [](const Json&, const Json& j) -> EResult<Json>
        {
            if(j.array_items().size() == 3
                    && j[1].is_number()
                    && j[2].is_number())
            {
                auto erd = Soil::bulkDensityClass2rawDensity(j[1].int_value(), j[2].number_value());
                if(erd.success())
                    return{erd.result};
                else
                    return{j, erd.errors};
            }
            return{j, string("Couldn't convert bulk density class to raw density using function: ") + j.dump() + "!"};
        };

        auto KA52clay = [](const Json&, const Json& j) -> EResult<Json>
        {
            if(j.array_items().size() == 2
                    && j[1].is_string())
            {
                auto ec = Soil::KA5texture2clay(j[1].string_value());
                if(ec.success())
                    return{ec.result};
                else
                    return{j, ec.errors};
            }
            return{j, string("Couldn't get soil clay content from KA5 soil class: ") + j.dump() + "!"};
        };

        auto KA52sand = [](const Json&, const Json& j) -> EResult<Json>
        {
            if(j.array_items().size() == 2
                    && j[1].is_string())
            {
                auto es = Soil::KA5texture2sand(j[1].string_value());
                if(es.success())
                    return{es.result};
                else
                    return{j, es.errors};
            }
            return{j, string("Couldn't get soil sand content from KA5 soil class: ") + j.dump() + "!"};;
        };

        auto sandClay2lambda = [](const Json&, const Json& j) -> EResult<Json>
        {
            if(j.array_items().size() == 3
                    && j[1].is_number()
                    && j[2].is_number())
                return{Soil::sandAndClay2lambda(j[1].number_value(), j[2].number_value())};
            return{j, string("Couldn't get lambda value from soil sand and clay content: ") + j.dump() + "!"};
        };
        */

        PatternFunc percent = (Map root, List args) -> {
            if(args.size() == 1
               && args.get(0) instanceof Number)
                return new Double((Double)args.get(0) / 100.0);
            if(errorPort.isConnected())
                errorPort.send(create("Couldn't convert percent to decimal percent value: " + args + "!"));
            return null;
        };

        supportedPatterns.put("include-from-file", fromFile);
        supportedPatterns.put("ref", ref);
        //supportedPatterns.put("humus_st2corg", humus2corg);
        //supportedPatterns.put("humus-class->corg", humus2corg);
        //supportedPatterns.put("ld_eff2trd", bdc2rd);
        //supportedPatterns.put("bulk-density-class->raw-density", bdc2rd);
        //supportedPatterns.put("KA5TextureClass2clay", KA52clay);
        //supportedPatterns.put("KA5-texture-class->clay", KA52clay);
        //supportedPatterns.put("KA5TextureClass2sand", KA52sand);
        //supportedPatterns.put("KA5-texture-class->sand", KA52sand);
        //supportedPatterns.put("sandAndClay2lambda", sandClay2lambda);
        //supportedPatterns.put("sand-and-clay->lambda", sandClay2lambda);
        supportedPatterns.put("%", percent);

    }

    @Override
    protected void execute() {
        //we got a valid origIn, try to set values
        Packet ip;
        while ((ip = inPort.receive()) != null) {
            PMap<String, Object> m = (PMap<String, Object>)ip.getContent();
            Object updatedMap = findAndReplaceReferences(m, m);
            if(PMap.class.isAssignableFrom(updatedMap.getClass()))
                m = (PMap<String, Object>)updatedMap;
            drop(ip);
            Packet out = create(m);
            outPort.send(out);
        }
    }

    @Override
    protected void openPorts() {
        inPort = openInput("IN");
        outPort = openOutput("OUT");
        errorPort = openOutput("ERROR");
    }

}
