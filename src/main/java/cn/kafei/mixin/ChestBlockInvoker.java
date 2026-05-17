package cn.kafei.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChestBlock.class)
public interface ChestBlockInvoker {
	@Invoker("getMenuProvider")
	MenuProvider quietly$getMenuProvider(BlockState state, Level level, BlockPos pos);
}
