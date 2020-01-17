package bl4ckscor3.mod.nomoresleeping;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

public class Configuration
{
	public static final ForgeConfigSpec CONFIG_SPEC;
	public static final Configuration CONFIG;

	public final BooleanValue disableSleepCompletely;

	static
	{
		Pair<Configuration,ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Configuration::new);

		CONFIG_SPEC = specPair.getRight();
		CONFIG = specPair.getLeft();
	}

	Configuration(ForgeConfigSpec.Builder builder)
	{
		disableSleepCompletely = builder
				.comment("true if sleeping should be completely disabled, false if only the wait after right-clicking a bed should be disabled.")
				.define("disableSleepCompletely", false);
	}
}
