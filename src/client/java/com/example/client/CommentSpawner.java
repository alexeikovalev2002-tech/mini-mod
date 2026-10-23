package com.example.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class CommentSpawner {
	private static final int LIFETIME_TICKS = 200; // 10 секунд
	private static final double RADIUS = 5.0;
	private static final int MAX_LENGTH = 60;
	private static final Random RANDOM = new Random();
	private static final List<Active> ACTIVE = new ArrayList<>();
	private static int nextFakeId = -1_000_000;

	private static class Active {
		final ArmorStandEntity stand;
		int ticksLeft = LIFETIME_TICKS;

		Active(ArmorStandEntity stand) {
			this.stand = stand;
		}
	}

	public static void spawn(String text) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		ClientPlayerEntity player = client.player;
		if (world == null || player == null) {
			return;
		}

		if (text.length() > MAX_LENGTH) {
			text = text.substring(0, MAX_LENGTH) + "...";
		}

		double x = player.getX() + (RANDOM.nextDouble() * 2 - 1) * RADIUS;
		double z = player.getZ() + (RANDOM.nextDouble() * 2 - 1) * RADIUS;
		double y = player.getY() + RANDOM.nextDouble() * 1.5;

		ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
		stand.setId(nextFakeId--);
		stand.setPosition(x, y, z);
		stand.setInvisible(true);
		stand.setNoGravity(true);
		stand.setCustomName(Text.literal(text));
		stand.setCustomNameVisible(true);

		world.addEntity(stand);
		ACTIVE.add(new Active(stand));
	}

	public static void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;

		if (world == null) {
			ACTIVE.clear();
			return;
		}

		Iterator<Active> it = ACTIVE.iterator();
		while (it.hasNext()) {
			Active active = it.next();
			active.ticksLeft--;
			if (active.ticksLeft <= 0) {
				world.removeEntity(active.stand.getId(), Entity.RemovalReason.DISCARDED);
				it.remove();
			}
		}
	}
}
