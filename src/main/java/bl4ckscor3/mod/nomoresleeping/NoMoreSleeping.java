package bl4ckscor3.mod.nomoresleeping;

import java.util.List;
import java.util.Optional;

import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerEntity.SleepResult;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@Mod(NoMoreSleeping.MODID)
@EventBusSubscriber(modid=NoMoreSleeping.MODID)
public class NoMoreSleeping
{
	public static final String MODID = "nomoresleeping";

	public NoMoreSleeping()
	{
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Configuration.CONFIG_SPEC);
	}

	@SubscribeEvent
	public static void onPlayerSleepInBed(PlayerSleepInBedEvent event)
	{
		if(!event.getPlayer().world.isRemote)
		{
			if(Configuration.CONFIG.disableSleepCompletely.get())
			{
				event.getPlayer().sendStatusMessage(new TranslationTextComponent("nomoresleeping.disabled"), true);
				event.setResult(SleepResult.OTHER_PROBLEM);
				return;
			}

			PlayerEntity player = event.getPlayer();
			ServerWorld world = (ServerWorld)player.world;
			BlockPos at = event.getPos();
			Direction direction = world.getBlockState(at).get(HorizontalBlock.HORIZONTAL_FACING);
			Optional<BlockPos> optAt = event.getOptionalPos();

			if(player.isSleeping() || !player.isAlive())
				event.setResult(SleepResult.OTHER_PROBLEM);
			else if(!world.dimension.isSurfaceWorld())
				event.setResult(SleepResult.NOT_POSSIBLE_HERE);
			else if(!ForgeEventFactory.fireSleepingTimeCheck(player, optAt))
				event.setResult(SleepResult.NOT_POSSIBLE_NOW);
			else if(!bedInRange(player, at, direction))
				event.setResult(SleepResult.TOO_FAR_AWAY);
			else if(isBedObstructed(player, at, direction))
				event.setResult(SleepResult.OBSTRUCTED);
			else
			{
				if(!player.isCreative())
				{
					List<MonsterEntity> list = world.getEntitiesWithinAABB(MonsterEntity.class, new AxisAlignedBB(at.getX() - 8.0D, at.getY() - 5.0D, at.getZ() - 8.0D, at.getX() + 8.0D, at.getY() + 5.0D, at.getZ() + 8.0D), e -> e.isPreventingPlayerRest(player));

					if(!list.isEmpty())
					{
						event.setResult(SleepResult.NOT_SAFE);
						return;
					}
				}

				if(world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE))
				{
					long l = world.getDayTime() + 24000L;
					world.setDayTime(ForgeEventFactory.onSleepFinished(world, l - l % 24000L, world.getDayTime()));
				}

				world.getPlayers().stream().filter(LivingEntity::isSleeping).forEach(p -> p.wakeUpPlayer(false, false, true));

				if(world.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE))
					world.dimension.resetRainAndThunder();

				event.setResult(SleepResult.OTHER_PROBLEM);
			}
		}
	}

	private static boolean bedInRange(PlayerEntity player, BlockPos pos, Direction dir)
	{
		if(Math.abs(player.posX - pos.getX()) <= 3.0D && Math.abs(player.posY - pos.getY()) <= 2.0D && Math.abs(player.posZ - pos.getZ()) <= 3.0D)
			return true;
		else if(dir == null)
			return false;
		else
		{
			BlockPos oppositePos = pos.offset(dir.getOpposite());
			return Math.abs(player.posX - oppositePos.getX()) <= 3.0D && Math.abs(player.posY - oppositePos.getY()) <= 2.0D && Math.abs(player.posZ - oppositePos.getZ()) <= 3.0D;
		}
	}

	private static boolean isBedObstructed(PlayerEntity player, BlockPos pos, Direction dir)
	{
		BlockPos posUp = pos.up();
		return !isNormalCube(player.world, posUp) || !isNormalCube(player.world, posUp.offset(dir.getOpposite()));
	}

	private static boolean isNormalCube(World world, BlockPos pos)
	{
		return !world.getBlockState(pos).causesSuffocation(world, pos);
	}
}
