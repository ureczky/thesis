package hu.ureczky.celebrations.map;

import android.graphics.Bitmap;

public class Tile
{
	// public for simplicity
	public int x;
	public int y;
	public Bitmap img;

	public Tile(int x, int y, Bitmap img)
	{
		this.x = x;
		this.y = y;
		this.img = img;
	}
}