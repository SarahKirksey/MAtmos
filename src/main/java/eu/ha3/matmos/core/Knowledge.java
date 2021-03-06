package eu.ha3.matmos.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;

import eu.ha3.matmos.Matmos;
import eu.ha3.matmos.core.event.Event;
import eu.ha3.matmos.core.expansion.ExpansionIdentity;
import eu.ha3.matmos.core.logic.Condition;
import eu.ha3.matmos.core.logic.Junction;
import eu.ha3.matmos.core.logic.Machine;
import eu.ha3.matmos.core.sheet.DataPackage;
import eu.ha3.matmos.core.sheet.Sheet;
import eu.ha3.matmos.core.sheet.SheetCommander;
import eu.ha3.matmos.core.sheet.SheetIndex;
import net.minecraft.client.resources.IResourcePack;

/**
 * Stores a Knowledge.
 */
public class Knowledge implements Evaluated, Simulated {
    private DataPackage data;

    private final Map<String, Dynamic> dynamicMapped = new TreeMap<>();
    private final Map<String, PossibilityList> possibilityMapped = new TreeMap<>();
    private final Map<String, Condition> conditionMapped = new TreeMap<>();
    private final Map<String, Junction> junctionMapped = new TreeMap<>();
    private final Map<String, Machine> machineMapped = new TreeMap<>();
    private final Map<String, Event> eventMapped = new TreeMap<>();

    private final SheetCommander<String> sheetCommander = new SheetCommander<String>() {
        @Override
        public int version(SheetIndex sheetIndex) {
            return data.getSheet(sheetIndex.getSheet()).version(sheetIndex.getIndex());
        }

        @Override
        public boolean listHas(String constantX, String value) {
            return possibilityMapped.containsKey(constantX) ? possibilityMapped.get(constantX).listHas(value) : false;
        }

        @Override
        public String get(SheetIndex sheetIndex) {
            return data.getSheet(sheetIndex.getSheet()).get(sheetIndex.getIndex());
        }

        @Override
        public boolean exists(SheetIndex sheetIndex) {
            return data.getSheet(sheetIndex.getSheet()).exists(sheetIndex.getIndex());
        }
    };

    private final Provider<Dynamic> dynamicProvider = new MappedProvider<>(dynamicMapped);
    private final Provider<Condition> conditionProvider = new MappedProvider<>(conditionMapped);
    private final Provider<Junction> junctionProvider = new MappedProvider<>(junctionMapped);
    private final Provider<Machine> machineProvider = new MappedProvider<>(machineMapped);
    private final Provider<Event> eventProvider = new MappedProvider<>(eventMapped);

    private final ProviderCollection providerCollection;

    //

    private final SoundRelay relay;

    public Knowledge(SoundRelay relay, ReferenceTime time) {
        this.relay = relay;

        providerCollection = new Providers(time, relay, sheetCommander, conditionProvider, junctionProvider, machineProvider, eventProvider, dynamicProvider);
    }

    public void setData(DataPackage data) {
        this.data = data;
    }

    public ProviderCollection obtainProviders() {
        return providerCollection;
    }

    public SheetCommander<String> obtainSheetCommander() {
        return sheetCommander;
    }

    public void addKnowledge(List<Named> namedThings) {
        for (Named n : namedThings) {
            if (n instanceof Condition) {
                conditionMapped.put(n.getName(), (Condition)n);
            } else if (n instanceof Junction) {
                junctionMapped.put(n.getName(), (Junction)n);
            } else if (n instanceof Machine) {
                machineMapped.put(n.getName(), (Machine)n);
            } else if (n instanceof Event) {
                eventMapped.put(n.getName(), (Event)n);
            } else if (n instanceof PossibilityList) {
                possibilityMapped.put(n.getName(), (PossibilityList)n);
            } else if (n instanceof Dynamic) {
                dynamicMapped.put(n.getName(), (Dynamic)n);
            } else {
                System.err.println("Cannot handle named element: " + n.getName() + " " + n.getClass());
            }
        }
    }

    public void compile() {
        purge(machineMapped, junctionMapped, "junctions");
        purge(junctionMapped, conditionMapped, "conditions");
    }

    /**
     * This method must return an object that can be modified afterwards by something else.
     */
    public Set<String> calculateRequiredModules() {
        return Streams.concat(
                conditionMapped.values().stream().flatMap(a -> a.getDependencies().stream()),
                dynamicMapped.values().stream().flatMap(a -> a.getDependencies().stream()))
            .collect(Collectors.toSet());
    }

    private void purge(Map<String, ? extends Dependable> superior, Map<String, ? extends Dependable> inferior, String inferiorName) {
        Set<String> requirements = new TreeSet<>();
        Set<String> unused = new TreeSet<>();
        Set<String> missing = new TreeSet<>();

        for (Dependable m : superior.values()) {
            requirements.addAll(m.getDependencies());
        }

        unused.addAll(inferior.keySet());
        unused.removeAll(requirements);

        missing.addAll(requirements);
        missing.removeAll(inferior.keySet());

        if (missing.size() > 0) {
            Matmos.LOGGER.warn("Missing " + inferiorName + ": " + Arrays.toString(missing.toArray()));
        }

        if (unused.size() > 0) {
            Matmos.LOGGER.warn("Unused " + inferiorName + ": " + Arrays.toString(unused.toArray()));

            unused.forEach(inferior::remove);
        }
    }

    public void cacheSounds(ExpansionIdentity identity) {
        IResourcePack resourcePack = identity.getPack();

        eventMapped.values().forEach(event -> event.cacheSounds(resourcePack));
    }

    @Override
    public void simulate() {
        relay.routine();
        machineMapped.values().forEach(Machine::simulate);
    }

    @Override
    public void evaluate() {
        if (dynamicMapped.size() > 0) {
            Sheet dynamic = data.getSheet(Dynamic.DEDICATED_SHEET);
            for (Dynamic o : dynamicMapped.values()) {
                o.evaluate();
                dynamic.set(o.getName(), Long.toString(o.getInformation()));
            }
        }

        conditionMapped.values().forEach(Evaluated::evaluate);
        junctionMapped.values().forEach(Evaluated::evaluate);
        machineMapped.values().forEach(Evaluated::evaluate);
    }
}
