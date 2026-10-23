package com.example.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Style;
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
		final Entity entity;
		int ticksLeft = LIFETIME_TICKS;

		Active(Entity entity) {
			this.entity = entity;
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
		double y = player.getY() + 0.5 + RANDOM.nextDouble() * 2.0;

		// случайный цвет рамки (RGB) с небольшой прозрачностью
		int r = RANDOM.nextInt(256);
		int g = RANDOM.nextInt(256);
		int b = RANDOM.nextInt(256);
		int background = (0xCC << 24) | (r << 16) | (g << 8) | b;

		// цвет текста: чёрный на светлом фоне, белый на тёмном
		double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
		int textColor = luminance > 140 ? 0x000000 : 0xFFFFFF;

		// пробелы и пустые строки вокруг текста делают рамку больше
		Text label = Text.literal(" \n   " + text + "   \n ")
			.setStyle(Style.EMPTY.withColor(textColor));

		DisplayEntity.TextDisplayEntity display =
			new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
		display.setId(nextFakeId--);
		display.setPosition(x, y, z);
		display.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
		display.setText(label);
		display.setBackground(background);

		world.addEntity(display);
		ACTIVE.add(new Active(display));
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
				world.removeEntity(active.entity.getId(), Entity.RemovalReason.DISCARDED);
				it.remove();
			}
		}
	}
}
