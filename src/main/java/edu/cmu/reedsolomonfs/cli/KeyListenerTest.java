package edu.cmu.reedsolomonfs.cli;

import javax.swing.*;
import java.awt.event.*;
 
@SuppressWarnings("serial")
public class KeyListenerTest extends JFrame {
	public KeyListenerTest() {
		MyWindow mywindow = new MyWindow();
		this.add(mywindow);
		this.addKeyListener(mywindow);// 注册监听器
		this.setSize(400, 400);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}
 
	public static void main(String[] args) {
		new KeyListenerTest();
	}
}
 
@SuppressWarnings("serial")
class MyWindow extends JPanel implements KeyListener {
	@Override
	public void keyTyped(KeyEvent e) {}
 
	// 按键监听器
	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println(e.getKeyChar()+"键被敲击");
	}
 
	// 释放监听器
	@Override
	public void keyReleased(KeyEvent e) {}
}