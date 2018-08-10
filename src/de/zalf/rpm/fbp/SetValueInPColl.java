package de.zalf.rpm.fbp;

import com.jpaulmorrison.fbp.core.engine.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.pcollections.*;

@ComponentDescription("Set a value in a PMap or PVector under the given path (which may contain keys for lists).")
@InPorts({
        @InPort(value = "VALUE", description = "Value to set"),
        @InPort(value = "PATH", description = "Path List<Object> or single key (Object == Index or String) to value in possibly nested datastructures"),
        @InPort(value = "COLL", description = "PMap<String, Object> or PVector<Object>"),
})
@OutPorts({
        @OutPort(value = "OUT", description = "PMap<String, Object> or PVector<Object>"),
        @OutPort(value = "ERROR", description = "Error message", type = String.class, optional = true)
})
public class SetValueInPColl extends Component {
    InputPort valuePort;
    InputPort pathPort;
    InputPort collPort;
    OutputPort outPort;
    OutputPort errorPort;

    PVector path;
    int pathLevel = 0;

    Object coll;
    int collLevel = 0;
    int sendCollOpenBrackets = 0;

    Object value;
    int valueLevel = 0;

    private boolean tryReceivingPath(){
        if(path == null || pathLevel >= collLevel) {
            Packet pp;
            while((pp = pathPort.receive()) != null) {
                if(pp.getType() == Packet.OPEN){
                    pathLevel++;
                    //sendCollOpenBrackets++;
                    drop(pp);
                } else if(pp.getType() == Packet.CLOSE){
                    pathLevel--;
                    //if(sendCollOpenBrackets > 0) {
                    //    outPort.send(create(Packet.CLOSE, ""));
                    //    sendCollOpenBrackets--;
                    //}
                    drop(pp);
                } else
                    break;
            }

            if(pp != null) {
                Object p = pp.getContent();
                if(p instanceof String)
                    path = Empty.vector().plus(p);
                else
                    path = TreePVector.from((List)pp.getContent());
                drop(pp);
                return true;
            }
        }
        return false;
    }

    private boolean tryReceivingColl(){
        if(coll == null || (collLevel >= pathLevel && collLevel >= valueLevel)) {
            Packet cp;
            while((cp = collPort.receive()) != null) {
                if (cp.getType() == Packet.OPEN) {
                    collLevel++;
                    drop(cp);
                } else if (cp.getType() == Packet.CLOSE) {
                    collLevel--;
                    drop(cp);
                } else
                    break;
            }

            if(cp != null) {
                coll = cp.getContent();
                drop(cp);
                return true;
            }
        }
        return false;
    }

    private boolean tryReceivingValue(){
        if(value == null || (valueLevel >= pathLevel && valueLevel >= collLevel)) {
            Packet vp;
            while((vp = valuePort.receive()) != null) {
                if (vp.getType() == Packet.OPEN) {
                    valueLevel++;
                    drop(vp);
                } else if (vp.getType() == Packet.CLOSE) {
                    valueLevel--;
                    drop(vp);
                } else
                    break;
            }

            if(vp != null) {
                value = vp.getContent();
                drop(vp);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void execute() {
        boolean someDataReceived = tryReceivingPath() || tryReceivingColl() || tryReceivingValue();

        if(!someDataReceived || path == null || coll == null || value == null)
            return;

        Object o;
        if(PMap.class.isAssignableFrom(coll.getClass()))
            o = updateIn((PMap<String, Object>)coll, path, value, Empty.vector());
        else if(PVector.class.isAssignableFrom(coll.getClass()))
            o = updateIn((PVector)coll, path, value, Empty.vector());
        else
            return;

        outPort.send(create(o));
    }

    //update in function for maps
    private PMap<String, Object> updateIn(PMap<String, Object> m, PVector<Object> path, Object value, PVector<Object> prevPath)
    {
        if(!path.isEmpty()) {
            Object p0 = path.get(0);
            if(p0 instanceof Integer){
                if(errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a VECTOR/LIST. Waiting for next IP.!"));
                return m;
            } else if(p0 instanceof String) {
                String key = (String)p0;
                PVector<Object> restPath = path.minus(0);
                if (m.containsKey(key)) {
                    if (restPath.isEmpty())
                        return m.plus(key, value);
                    else {
                        Object o = m.get(key);
                        if (Map.class.isAssignableFrom(o.getClass())) {
                            return m.plus(key, updateIn(HashTreePMap.from((Map<String, Object>)o), restPath, value, prevPath.plus(p0)));
                        } else if (Collection.class.isAssignableFrom(o.getClass())) {
                            return m.plus(key, updateIn(TreePVector.from((Collection<Object>)o), restPath, value, prevPath.plus(p0)));
                        } else if (errorPort.isConnected())
                            errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP or VECTOR/LIST. Waiting for next IP.!"));
                    }
                } else if (errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a value. Waiting for next IP.!"));
            }
        }
        return m;
    }

    //update in function for linear collections
    private PVector<Object> updateIn(PVector<Object> v, PVector<Object> path, Object value, PVector<Object> prevPath)
    {
        if(!path.isEmpty()) {
            Object p0 = path.get(0);
            if (p0 instanceof String) {
                if (errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP. Waiting for next IP.!"));
                return v;
            } else if (p0 instanceof Integer) {
                int index = (Integer)p0;
                PVector<Object> restPath = path.minus(0);
                if (index < v.size()) {
                    if (restPath.isEmpty())
                        return v.with(index, value);
                    else {
                        Object o = v.get(index);
                        if (Map.class.isAssignableFrom(o.getClass())) {
                            return v.with(index, updateIn(HashTreePMap.from((Map<String, Object>)o), restPath, value, prevPath.plus(p0)));
                        } else if (Collection.class.isAssignableFrom(o.getClass())) {
                            return v.with(index, updateIn(TreePVector.from((Collection<Object>)o), restPath, value, prevPath.plus(p0)));
                        } else if (errorPort.isConnected())
                            errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a MAP or VECTOR/LIST. Waiting for next IP.!"));
                    }
                } else if (errorPort.isConnected())
                    errorPort.send(create("Path [" + prevPath.plus(p0) + "] doesn't designate a value. Waiting for next IP.!"));
            }
        }
        return v;
    }

    @Override
    protected void openPorts() {
        valuePort = openInput("VALUE");
        pathPort = openInput("PATH");
        collPort = openInput("COLL");
        outPort = openOutput("OUT");
        errorPort = openOutput("ERROR");
    }
}
