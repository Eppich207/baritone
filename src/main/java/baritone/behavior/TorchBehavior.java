/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.ITorchBehavior;
import baritone.api.event.events.TickEvent;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TorchBehavior extends Behavior implements ITorchBehavior {

    private Vec3 lastTorchPos = null;
    private int cooldown = 0;

    public TorchBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() != TickEvent.Type.IN) {
            return;
        }
        if (!Baritone.settings().autoTorch.value) {
            return;
        }
        if (ctx.player() == null || ctx.world() == null) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Vec3 pos = ctx.player().position();
        int interval = Baritone.settings().autoTorchInterval.value;

        if (lastTorchPos != null) {
            double dx = pos.x - lastTorchPos.x;
            double dz = pos.z - lastTorchPos.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist < interval) {
                return;
            }
        }

        // Find a torch in the hotbar/inventory and switch to it
        boolean hasTorch = baritone.getInventoryBehavior().throwaway(true, stack -> stack.getItem() == Items.TORCH);
        if (!hasTorch) {
            return;
        }

        // Place torch on the floor block below player's feet
        BetterBlockPos feet = ctx.playerFeet();
        BlockPos floorBlock = feet.below();

        // Only place if the floor block is solid and the feet-level block is air
        if (!ctx.world().getBlockState(floorBlock).isSolid()) {
            return;
        }
        if (!ctx.world().getBlockState(feet).isAir()) {
            return;
        }

        Vec3 hitVec = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, floorBlock, false);

        InteractionResult result = ctx.playerController().processRightClickBlock(
                ctx.player(), ctx.world(), InteractionHand.MAIN_HAND, hitResult);

        if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
            ctx.player().swing(InteractionHand.MAIN_HAND);
            lastTorchPos = pos;
            cooldown = 5;
        }
    }

    @Override
    public void resetTorchTracking() {
        lastTorchPos = null;
    }
}
