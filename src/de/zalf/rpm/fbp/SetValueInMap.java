package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.Collection;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Set a value in a MAP under the given path (which may contain indices for lists).")
@InPorts({
        @InPort(value = "VALUE", description = "Value to set"),
        @InPort(value = "PATH", description = "Path (a string) to value in possibly nested maps", type = String.class),
        @InPort(value = "MAP", description = "Map representing JSON Object", type = PMap.class),
})
@OutPorts({
        @OutPort(value = "OUT", description = "Map representation of JSON object", type = PMap.class),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class SetValueInMap extends Component {
    InputPort valuePort;
    InputPort pathPort;
    InputPort mapPort;
    OutputPort outPort;
    OutputPort errorPort;

    class SorI {
        SorI(String k){ key = k; }
        SorI(int i){ index = i; }
        String key;
        int index = -1;
        boolean isIndex() { return index >= 0; }
        boolean isStringKey() { return !isIndex(); }
        public String toString() { return index < 0 ? "" + index : key; }
    }

    String pathsIIP;
    PVector<SorI> path;
    HashPMap<String, Object> currentMap;

    @Override
    protected void execute() {
        //read IIP for path
        if (!pathPort.isClosed()) {
            Packet p = pathPort.receive();
            if (p == null)
                return;
            pathsIIP = (String)p.getContent();
            path = Empty.vector();
            for(String s : pathsIIP.split(",")) {
                String st = s.trim();
                try {
                    int i = Integer.parseInt(st);
                    path = path.plus(new SorI(i));
                } catch (NumberFormatException nfe){
                    path = path.plus(new SorI(st));
                }
            }
            drop(p);
            pathPort.close();
        }

        //try to read a map packet
        Packet mp = mapPort.receive();
        if (mp == null) {
            //makes no sense to continue (read values) without map, so close the component
            if(errorPort.isConnected())
                errorPort.send(create("No MAPs available. Terminating process!"));
            mapPort.close();
            valuePort.close();
            return;
        }
        currentMap = (HashPMap<String, Object>) mp.getContent();
        drop(mp);
        //if message content was no map, wait for next IP on MAP port
        if(currentMap == null) {
            if(errorPort.isConnected())
                errorPort.send(create("IP was not of type HashPMap. Waiting for next IP.!"));
            return;
        }

        //we got a valid map, try to set values
        Packet vp;
        while ((vp = valuePort.receive()) != null) {
            if(vp.getType() == Packet.OPEN) {
                drop(vp);
                continue;
            } else if(vp.getType() == Packet.CLOSE) {
                drop(vp);
                currentMap = null;
                //deactivate component and continue with a new map
                return;
            }
            Object v = vp.getContent();
            HashPMap<String, Object> m = updateIn(currentMap, path, v, Empty.vector());
            drop(vp);
            Packet out = create(m);
            outPort.send(out);
        }
    }

    //update in function for maps
    private HashPMap<String, Object> updateIn(HashPMap<String, Object> m, PVector<SorI> path, Object value,
                                              PVector<SorI> prevPath)
    {
        if(!path.isEmpty()) {
            SorI p0 = path.get(0);
            if(p0.isIndex()){
                if(errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a VECTOR/LIST. Waiting for next IP.!"));
                return m;
            }
            String key = p0.key;
            PVector<SorI> restPath = path.minus(0);
            if (m.containsKey(key)) {
                if (restPath.isEmpty())
                    return m.plus(key, value);
                else {
                    Object o = m.get(key);
                    if(Map.class.isAssignableFrom(o.getClass())){
                        return m.plus(key, updateIn(HashTreePMap.from((Map)o), restPath, value, prevPath.plus(p0)));
                    } else if(Collection.class.isAssignableFrom(o.getClass())) {
                        return m.plus(key, updateIn(TreePVector.from((Collection)o), restPath, value, prevPath.plus(p0)));
                    } else if(errorPort.isConnected())
                        errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP or VECTOR/LIST. Waiting for next IP.!"));
                }
            } else if(errorPort.isConnected())
                errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a value. Waiting for next IP.!"));
        }
        return m;
    }

    //update in function for linear collections
    private PVector<Object> updateIn(PVector<Object> v, PVector<SorI> path, Object value, PVector<SorI> prevPath)
    {
        if(!path.isEmpty()) {
            SorI p0 = path.get(0);
            if(p0.isStringKey()){
                if(errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP. Waiting for next IP.!"));
                return v;
            }
            int index = p0.index;
            PVector<SorI> restPath = path.minus(0);
            if (index < v.size()) {
                if (restPath.isEmpty())
                    return v.with(index, value);
                else {
                    Object o = v.get(index);
                    if(Map.class.isAssignableFrom(o.getClass())){
                        return v.with(index, updateIn(HashTreePMap.from((Map)o), restPath, value, prevPath.plus(p0)));
                    } else if(Collection.class.isAssignableFrom(o.getClass())) {
                        return v.with(index, updateIn(TreePVector.from((Collection)o), restPath, value, prevPath.plus(p0)));
                    } else if(errorPort.isConnected())
                        errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP or VECTOR/LIST. Waiting for next IP.!"));
                }
            } else if(errorPort.isConnected())
                errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a value. Waiting for next IP.!"));
        }
        return v;
    }

    @Override
    protected void openPorts() {
        valuePort = openInput("VALUE");
        pathPort = openInput("PATH");
        mapPort = openInput("MAP");
        outPort = openOutput("OUT");
        errorPort = openOutput("ERROR");
    }
}
