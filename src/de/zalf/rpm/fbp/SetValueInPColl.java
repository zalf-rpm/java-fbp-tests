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
    private InputPort valuePort;
    private InputPort pathPort;
    private InputPort collPort;
    OutputPort outPort;
    private OutputPort errorPort;

    private PVector path;
    private boolean receiveNextPath = true;
    private boolean pathEarlyClose = false;
    private int pathLevel = 0;

    private Object coll;
    private boolean receiveNextColl = true;

    Object value;
    private boolean receiveNextValue = true;
    private boolean valueEarlyClose = false;
    private int valueLevel = 0;


    private void tryReceivingPath(){
        //allow receives only if a new path is requested and we're still at the same or a higher
        //substream level
        if(receiveNextPath && !pathEarlyClose) {
            Packet pp;
            while((pp = pathPort.receive()) != null) {
                if(pp.getType() == Packet.OPEN){
                    pathLevel++;
                    drop(pp);
                } else if(pp.getType() == Packet.CLOSE){
                    pathLevel--;
                    drop(pp);
                    // if the levels are equal again at this point, this means that
                    // the valuePort already received a CLOSE bracket, so we can send
                    // the updated COLL
                    // else we allow the valuePort to keep on receiving values within
                    // the same substream, but always update the same path, which will
                    // effectively overwrite the previous update
                    // (if both substreams have different lengths)
                    if(pathLevel == valueLevel) {
                        outPort.send(create(coll));
                        receiveNextColl = true;
                        receiveNextValue = true;
                        valueEarlyClose = false;
                    } else {
                        receiveNextPath = false;
                        pathEarlyClose = true;
                    }
                    return;
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
                receiveNextPath = false;
            }
        }
    }

    private void tryReceivingColl(){
        if(receiveNextColl) {
            Packet cp;
            while((cp = collPort.receive()) != null) {
                if (cp.getType() == Packet.OPEN) {
                    if (errorPort.isConnected())
                        errorPort.send(create("(Open)-Brackets have no semantics for COLL port! Ignoring open bracket."));
                    drop(cp);
                } else if (cp.getType() == Packet.CLOSE) {
                    if (errorPort.isConnected())
                        errorPort.send(create("(Close)-Brackets have no semantics for COLL port! Ignoring close bracket."));
                    drop(cp);
                    return;
                } else
                    break;
            }

            if(cp != null) {
                coll = cp.getContent();
                drop(cp);
                receiveNextColl = false;
            }
        }
    }

    private void tryReceivingValue(){
        //allow receives only if a new value is requested and we're still at the same or a higher
        //substream level
        if(receiveNextValue && !valueEarlyClose) {
            Packet vp;
            while((vp = valuePort.receive()) != null) {
                if (vp.getType() == Packet.OPEN) {
                    valueLevel++;
                    drop(vp);
                } else if (vp.getType() == Packet.CLOSE) {
                    valueLevel--;
                    drop(vp);
                    // if the levels are equal again at this point, this means that
                    // the pathPort already received a CLOSE bracket, so we can send
                    // the updated COLL
                    // else we allow the pathPort to keep on receiving paths within
                    // the same substream, but always update the same value on a different path
                    // (if both substreams have different lengths)
                    if(valueLevel == pathLevel) {
                        outPort.send(create(coll));
                        receiveNextColl = receiveNextPath = true;
                        pathEarlyClose = false;
                    } else {
                        receiveNextValue = false;
                        valueEarlyClose = true;
                    }
                    return;
                } else
                    break;
            }

            if(vp != null) {
                value = vp.getContent();
                drop(vp);
                receiveNextValue = false;
            }
        }
    }

    @Override
    protected void execute() {
        tryReceivingPath();
        tryReceivingColl();
        tryReceivingValue();

        //if fields aren't yet initialized we can't continue
        if(path == null || coll == null || value == null)
            return;

        //to continue all values have to be in sync = path fits to value and there's a coll, then the map can be updated
        if(receiveNextPath || receiveNextColl || receiveNextValue)
            return;

        if(PMap.class.isAssignableFrom(coll.getClass()))
            coll = updateIn((PMap<String, Object>)coll, path, value, Empty.vector());
        else if(PVector.class.isAssignableFrom(coll.getClass()))
            coll = updateIn((PVector)coll, path, value, Empty.vector());

        //update path and value
        receiveNextPath = receiveNextValue = true;
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
