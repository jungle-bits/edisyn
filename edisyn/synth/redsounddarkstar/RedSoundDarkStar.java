/***
    Copyright 2019 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package edisyn.synth.redsounddarkstar;

import edisyn.*;
import edisyn.gui.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.midi.*;


/**
   A patch editor for the Red Sound Darkstar.
        
   @author Sean Luke and Bryan Hoyle
*/

public class RedSoundDarkStar extends Synth
    {
    public static final String[] LFO_WAVES = { "Ramp", "Triangle", "Square", "Sine", "Pulse", "Sample & Hold", "Random" };
    public static final String[] MIDI_SYNC = { "Off", "1/8 beat", "1/6 beat", "1/4 beat", "1/3 beat", "1/2 beat", "2/3 beat", "3/4 beat", "1 beat", "2 beats", "3 beats", "1 bar", "1.5 bars", "2 bars", "3 bars", "4 bars" };
    public static final String[] MOD_SOURCES = { "Envelope 1", "Envelope 2", "LFO 1", "LFO 2" };
    public static final String[] PITCH_OFFSETS = { "None", "Fourth", "Fifth", "Octave", "Octave + Major 3rd", "Octave + Fourth", "Octave + Fifth", "2 Octaves" };
    public static final String[] OSCILLATOR_SOURCES = { "Normal", "Formant", "White Noise", "Pink Noise", "Blue Noise", "Eternal 1", "External 2" };
    public static final String[] KEY_TRACKING = { "Off", "25%", "50%", "100%", "150%", "-25%", "-50%" };
    public static final String[] FILTER_TYPES = { "Low Pass", "Band Pass", "High Pass" };
    public static final String[] TREMELO_SOURCES = { "LFO 1", "LFO 2" };
    public static final String[] PAN_MOD_SOURCES = { "Pot", "Envelope 1", "Envelope 2", "LFO 1", "LFO 2" };
    public static final String[] PORTAMENTO_TYPES = { "Off", "Type 1", "Type 2", "Pre-glide 1", "Pre-glide 2", "Pre-glide 3", "Pre-glide 4", "Pre-glide 5", "Pre-glide 6" };
    public static final String[] JOYSTICK_ASSIGNMENTS = { "Off", "X: Filter Freq  Y: Resonance", "X: Mix  Y: Ring Mod  [XP2]", "X: Env1 Attack  Y: Decay  [XP2]", "X: Env2 Attack  Y: Decay  [XP2]", "X: LFO1 Speed  Y: LFO2 Speed  [XP2]" };
    public static final String[] OUTPUT_ASSIGNMENTS = { "Main Outputs", "Aux Outputs" };
    public static final String[] AUDITION_TYPES = { "Arp 3", "Note 1", "Note 2", "Note 3", "Bass 1", "Bass 2", "Bass 3", "Bass Drum", "Chord 1", "Chord 2", "Snare", "Arp 1", "Arp 2" };
    public static final String[] PARTS = { "Part 1", "Part 2", "Part 3", "Part 4", "Part 5" };

    boolean xp2;
    public static final String XP2_KEY = "XP2";
    
    public boolean isXP2() { return xp2; }
    public void setXP2(boolean val)
        {
        setLastX("" + (!val), XP2_KEY, getSynthName(), true);
        xp2 = val;
        updateTitle();
        }
    

    public JFrame sprout()
        {
        JFrame frame = super.sprout();
        blend.setEnabled(false);
        getAll.setEnabled(false);
        //merge.setEnabled(false);
        transmitTo.setEnabled(false);
        receiveCurrent.setEnabled(false);
        receivePatch.setEnabled(false);
        receiveNextPatch.setEnabled(false);
        transmitParameters.setEnabled(false);
        transmitParameters.setSelected(false);
        addDarkStarMenu();
        return frame;
        }         

    public void addDarkStarMenu()
        {
        JMenu menu = new JMenu("DarkStar");
        menubar.add(menu);

        JMenuItem partmenu = new JMenuItem("Swap Parts...");
        partmenu.addActionListener(new ActionListener()
            {
            JComboBox part1 = new JComboBox(PARTS);
            JComboBox part2 = new JComboBox(PARTS);
                
            public void actionPerformed(ActionEvent e)
                {
                boolean result = showMultiOption(RedSoundDarkStar.this, new String[] { "Swap", "With" }, 
                    new JComponent[] { part1, part2 }, "Swap Parts...", "Enter the parts to swap with one another.");
                                
                if (result)
                    {
                    int p1 = part1.getSelectedIndex();
                    int p2 = part2.getSelectedIndex();
                    for(int i = 0; i < BASIC_PARAMETERS.length; i++)
                        {
                        int temp = model.get("part" + (p1 + 1) + BASIC_PARAMETERS[i], 0);
                        model.set("part" + (p1 + 1) + BASIC_PARAMETERS[i],
                            model.get("part" + (p2 + 1) + BASIC_PARAMETERS[i], 0));
                        model.set("part" + (p2 + 1) + BASIC_PARAMETERS[i], temp);
                        }
                    sendAllParameters();
                    }
                }                               
            });

        menu.add(partmenu);

        partmenu = new JMenuItem("Copy Part To...");
        partmenu.addActionListener(new ActionListener()
            {
            JComboBox part1 = new JComboBox(PARTS);
            JComboBox part2 = new JComboBox(PARTS);
                
            public void actionPerformed(ActionEvent e)
                {
                boolean result = showMultiOption(RedSoundDarkStar.this, new String[] { "Copy", "To" }, 
                    new JComponent[] { part1, part2 }, "Copy Part To...", "Enter the part to copy, and where to copy it.");
                                
                if (result)
                    {
                    int p1 = part1.getSelectedIndex();
                    int p2 = part2.getSelectedIndex();
                    for(int i = 0; i < BASIC_PARAMETERS.length; i++)
                        {
                        int temp = model.get("part" + (p1 + 1) + BASIC_PARAMETERS[i], 0);
                        model.set("part" + (p2 + 1) + BASIC_PARAMETERS[i], temp);
                        }
                    sendAllParameters();
                    }
                }                               
            });

        menu.add(partmenu);
        }

    public RedSoundDarkStar()
        {
        model.set("number", 0);
                
        String m = getLastX(XP2_KEY, getSynthName());
        xp2 = (m == null ? false : !Boolean.parseBoolean(m));

        VBox vbox = new VBox();
        HBox hbox = new HBox();

        for(int i = 1; i <= 5; i++)
            {
            JComponent sourcePanel = new SynthPanel(this);
            //            ((SynthPanel)sourcePanel).makePasteable("part" + i);
            ((SynthPanel)sourcePanel).makePasteable("part");
            
            vbox = new VBox();
                
            hbox = new HBox();
            if (i == 1)
                {
                hbox.add(addNameGlobal(Style.COLOR_GLOBAL()));
                }
                                
            hbox.addLast(addGeneral(i, Style.COLOR_A()));
            vbox.add(hbox);
                        
            hbox = new HBox();
            hbox.add(addOscillatorsGeneral(i, Style.COLOR_B()));
            hbox.addLast(addOscillator(i, 1, Style.COLOR_B()));
            vbox.add(hbox);
            hbox = new HBox();
            hbox.add(addLFO(i, 1, Style.COLOR_A()));
            hbox.add(addOscillator(i, 2, Style.COLOR_B()));
            vbox.add(hbox);
            hbox = new HBox();
            hbox.add(addLFO(i, 2, Style.COLOR_A()));
            hbox.addLast(addFilter(i, Style.COLOR_C()));
            vbox.add(hbox);
                        
            hbox = new HBox();
            hbox.add(addEnvelope(i, 1, Style.COLOR_A()));
            hbox.addLast(addEnvelope(i, 2, Style.COLOR_A()));
            vbox.add(hbox);
            vbox.add(addModulation(i, Style.COLOR_A()));
                        
            sourcePanel.add(vbox, BorderLayout.CENTER);
            if (i == 1)
                {
                addTab("Global and Part 1", sourcePanel);
                }
            else
                {
                addTab("Part " + i, sourcePanel);
                }
            }

        model.set("name", "INIT VOICE");
        
        loadDefaults();        
        }
                
    public String getDefaultResourceFileName() { return "RedSoundDarkStar.init"; }
    public String getHTMLResourceFileName() { return "RedSoundDarkStar.html"; }

    public boolean gatherPatchInfo(String title, Model change, boolean writing)
        {
        JTextField number = new SelectedTextField("" + (model.get("number") + 1), 3);

        while(true)
            {
            boolean result = showMultiOption(this, new String[] { "Patch Number"}, 
                new JComponent[] { number }, title, "Enter the Patch number");
                
            if (result == false) 
                return false;
                                
            int n;
            try { n = Integer.parseInt(number.getText()); }
            catch (NumberFormatException e)
                {
                showSimpleError(title, "The Patch Number must be an integer 1...64");
                continue;
                }
            if (n < 1 || n > 64)
                {
                showSimpleError(title, "The Patch Number must be an integer 1...64");
                continue;
                }
                
            n--;
                                
            change.set("number", n);
                        
            return true;
            }
        }
                                    
    /** Add the global patch category (name, id, number, etc.) */
    public JComponent addNameGlobal(Color color)
        {
        Category globalCategory = new Category(this, getSynthName(), color);
        //globalCategory.makeUnresettable();
                
        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
                
        VBox vbox = new VBox();
        comp = new PatchDisplay(this, 2);
        vbox.add(comp);

        HBox hbox2 = new HBox();
        final JCheckBox check = new JCheckBox("XP2");
        check.setSelected(xp2);
        check.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                setXP2(check.isSelected());
                }
            });
        check.setFont(Style.SMALL_FONT());
        check.setOpaque(false);
        check.setForeground(Style.TEXT_COLOR());
        hbox2.add(check);
        hbox2.add(Stretch.makeHorizontalStretch());
        vbox.add(Stretch.makeVerticalStretch());
        vbox.add(hbox2);
        hbox.add(vbox);

        comp = new LabelledDial("Current", this, "currenteditpart", color, 0, 4);
        ((LabelledDial)comp).addAdditionalLabel("Edit Part");
        hbox.add(comp);
        
        comp = new LabelledDial("Chorus", this, "chorusdepth", color, 0, 15);
        ((LabelledDial)comp).addAdditionalLabel("Depth [XP2]");
        hbox.add(comp);
        
        comp = new LabelledDial("Chorus", this, "chorusrate", color, 0, 15);
        ((LabelledDial)comp).addAdditionalLabel("Rate [XP2]");
        hbox.add(comp);
        
        
        // Not enough space to show the title
        //        hbox.addLast(Strut.makeHorizontalStrut(150));

        globalCategory.add(hbox, BorderLayout.WEST);
        return globalCategory;
        }

    public JComponent addOscillatorsGeneral(int part, Color color)
        {
        Category category = new Category(this, "Oscillators", color);
        //        category.makePasteable("part" + part);
        category.makePasteable("part");

        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
        
        VBox vbox = new VBox();
        params = TREMELO_SOURCES;
        comp = new Chooser("Tremelo Mod Source", this, "part" + part + "tremelomodsource", params);
        vbox.add(comp);

        params = PAN_MOD_SOURCES;
        comp = new Chooser("Pan Mod Source", this, "part" + part + "panmodsource", params);
        vbox.add(comp);
        
        hbox.add(vbox);

        comp = new LabelledDial("Volume", this, "part" + part + "volume", color, 0, 127);
        hbox.add(comp);
        
        comp = new LabelledDial("Pan", this, "part" + part + "pan", color, 0, 127)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 64; }

            public String map(int val)
                {
                if (val == 64) return "--";
                else if (val < 64) return "< " + (64 - val);
                else return "" + (val - 64) + " >";
                }
            };
        hbox.add(comp);
        
        comp = new LabelledDial("Tremelo", this, "part" + part + "tremelo", color, 0, 127, 64);
        hbox.add(comp);
        
        comp = new LabelledDial("Mix", this, "part" + part + "mix", color, 0, 127)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 64; }

            public String map(int val)
                {
                if (val == 64) return "--";
                else if (val < 64) return "< " + (64 - val);
                else return "" + (val - 64) + " >";
                }
            };
        hbox.add(comp);
        
        comp = new LabelledDial("Ring", this, "part" + part + "ring", color, 0, 127);
        hbox.add(comp);
        
        category.add(hbox, BorderLayout.CENTER);
        return category;
        }




    public JComponent addGeneral(int part, Color color)
        {
        Category category = new Category(this, "General", color);
        //        category.makePasteable("part" + part);
        category.makePasteable("part");

        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
        
        VBox vbox = new VBox();
        params = PORTAMENTO_TYPES;
        comp = new Chooser("Portamento Type", this, "part" + part + "portamentotype", params);
        vbox.add(comp);

        comp = new CheckBox("Sustain", this, "part" + part + "sustain");
        vbox.add(comp);

        comp = new CheckBox("Portamento Auto Glide", this, "part" + part + "autoglide");
        vbox.add(comp);

        hbox.add(vbox);

        comp = new LabelledDial("Bend Range", this, "part" + part + "bendrange", color, 0, 12);
        hbox.add(comp);
        
        comp = new LabelledDial("Channel", this, "part" + part + "channel", color, 0, 15, -1);
        hbox.add(comp);
        
        comp = new LabelledDial("Polyphony", this, "part" + part + "polyphony", color, 0, 8);
        hbox.add(comp);
        
        comp = new LabelledDial("Note Lo", this, "part" + part + "notelo", color, 0, 127);
        hbox.add(comp);
        
        comp = new LabelledDial("Note Hi", this, "part" + part + "notehi", color, 0, 127);
        hbox.add(comp);
        
        comp = new LabelledDial("Transpose", this, "part" + part + "transpose", color, 0, 48, 24)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 24; }
            };
                        
        hbox.add(comp);
        
        comp = new LabelledDial("Portamento", this, "part" + part + "portamentotime", color, 0, 127);
        ((LabelledDial)comp).addAdditionalLabel("Time");
        hbox.add(comp);

        comp = new LabelledDial("FX Send", this, "part" + part + "fxsend", color, 0, 15);
        ((LabelledDial)comp).addAdditionalLabel("[XP2]");
        hbox.add(comp);
        
        
        
        category.add(hbox, BorderLayout.CENTER);
        return category;
        }




    public JComponent addModulation(int part, Color color)
        {
        Category category = new Category(this, "Modulation", color);
        //        category.makePasteable("part" + part);
        category.makePasteable("part");

        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
        
        VBox vbox = new VBox();
        params = JOYSTICK_ASSIGNMENTS;
        comp = new Chooser("Joystick Assign", this, "part" + part + "joystickassign", params);
        vbox.add(comp);

        HBox hbox2 = new HBox();
        params = OUTPUT_ASSIGNMENTS;
        comp = new Chooser("Output Assign [XP2]", this, "part" + part + "outputassign", params);
        hbox2.add(comp);
        
        params = AUDITION_TYPES;
        comp = new Chooser("Audition Type [XP2]", this, "part" + part + "auditiontype", params);
        hbox2.add(comp);
        vbox.add(hbox2);
        hbox.add(vbox);
        
        vbox = new VBox();

        comp = new CheckBox("Part Output Shift", this, "part" + part + "partoutputshift");
        vbox.add(comp);

        comp = new CheckBox("Filter Shift", this, "part" + part + "filtershift");
        vbox.add(comp);
        hbox.add(vbox);
                
        vbox = new VBox();
        comp = new CheckBox("Env Shift", this, "part" + part + "envshift");
        vbox.add(comp);

        comp = new CheckBox("Env 1/2 Shift", this, "part" + part + "envhalfshift");
        ((CheckBox)comp).addToWidth(2);
        vbox.add(comp);

        comp = new CheckBox("Osc Shift", this, "part" + part + "oscshift");
        vbox.add(comp);

        hbox.add(vbox);
                
        vbox = new VBox();
        comp = new CheckBox("Osc 1/2 Shift", this, "part" + part + "oschalfshift");
        ((CheckBox)comp).addToWidth(2);
        vbox.add(comp);

        comp = new CheckBox("LFO Shift", this, "part" + part + "lfoshift");
        vbox.add(comp);

        comp = new CheckBox("LFO 1/2 Shift", this, "part" + part + "lfohalfshift");
        ((CheckBox)comp).addToWidth(2);
        vbox.add(comp);


        hbox.add(vbox);
                
        comp = new LabelledDial("Aftertouch", this, "part" + part + "aftertouchpitch", color, 0, 14, 7)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 7; }
            };
                
        ((LabelledDial)comp).addAdditionalLabel("Pitch");
        hbox.add(comp);
        
        comp = new LabelledDial("Aftertouch", this, "part" + part + "aftertouchfilter", color, 0, 14, 7)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 7; }
            };
                
        ((LabelledDial)comp).addAdditionalLabel("Filter");
        hbox.add(comp);
        
        comp = new LabelledDial("Mod Wheel", this, "part" + part + "modwheelpitch", color, 0, 14, 7)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 7; }
            };
                
        ((LabelledDial)comp).addAdditionalLabel("Pitch");
        hbox.add(comp);
        
        comp = new LabelledDial("Mod Wheel", this, "part" + part + "modwheelfilter", color, 0, 14, 7)
            {
            public boolean isSymmetric() { return true; }
            public int getDefaultValue() { return 7; }
            };
                
        ((LabelledDial)comp).addAdditionalLabel("Filter");
        hbox.add(comp);
        
        
        category.add(hbox, BorderLayout.CENTER);
        return category;
        }



    public JComponent addLFO(int part, int lfo, Color color)
        {
        Category category = new Category(this, "LFO " + lfo + (lfo == 2 ? " (Filter Frequency)" : ""), color);
        //        category.makePasteable("part" + part + "lfo" + lfo);
        category.makePasteable("part" + part + "lfo");

        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
        
        VBox vbox = new VBox();
        params = LFO_WAVES;
        comp = new Chooser("Shape", this, "part" + part + "lfo" + lfo + "shape", params);
        vbox.add(comp);
        HBox hbox2 = new HBox();
        params = MIDI_SYNC;
        comp = new Chooser("MIDI Sync", this, "part" + part + "lfo" + lfo + "midisync", params);
        hbox2.add(comp);
        comp = new CheckBox("Sync", this, "part" + part + "lfo" + lfo + "sync");
        ((CheckBox)comp).addToWidth(2);
        hbox2.add(comp);
        vbox.add(hbox2);
        hbox.add(vbox);

        comp = new LabelledDial("Speed", this, "part" + part + "lfo" + lfo + "speed", color, 0, 127);
        hbox.add(comp);
        
        comp = new LabelledDial("Delay", this, "part" + part + "lfo" + lfo + "delay", color, 0, 127);
        hbox.add(comp);
        
        category.add(hbox, BorderLayout.CENTER);
        return category;
        }
    

    public JComponent addEnvelope(int part, int env, Color color)
        {
        Category category = new Category(this, "Envelope " + env + (env == 1 ? " (Amplitude)" : " (Filter Frequency)"), color);
        //        category.makePasteable("part" + part + "env" + env);
        category.makePasteable("part" + part + "env");
                
        JComponent comp;
        String[] params;
        HBox hbox = new HBox();
               

        comp = new LabelledDial("Attack", this, "part" + part +  "env" + env + "attack", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Decay", this, "part" + part +  "env" + env + "decay", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Sustain", this, "part" + part +  "env" + env + "sustain", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Release", this, "part" + part +  "env" + env + "release", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Velocity", this, "part" + part +  "env" + env + "velocity", color, 0, 7);
        hbox.add(comp);


        // ADSR
        comp = new EnvelopeDisplay(this, Style.ENVELOPE_COLOR(), 
            new String[] { null,   "part" + part + "env" + env + "attack",   "part" + part + "env" + env + "decay",  null,   "part" + part + "env" + env + "release" },
            new String[] {   null,  null,   "part" + part + "env" + env + "sustain",    "part" + part + "env" + env + "sustain",  null },
            new double[] { 0, 0.25/127.0, 0.25 / 127.0,  0.25, 0.25/127.0},
            new double[] { 0, 1.0, 1.0 / 127.0, 1.0/127.0, 0 });
        hbox.addLast(comp);
                
        category.add(hbox, BorderLayout.CENTER);
        return category;
        }
        




    public JComponent addOscillator(int part, int osc, Color color)
        {
        final Category category = new Category(this, "Oscillator " + osc, color);
        //        category.makePasteable("part" + part + "osc" + osc);
        category.makePasteable("part" + part + "osc");

        JComponent comp;
        String[] params;
        final HBox hbox = new HBox();
        
        if (osc == 2)
            {
            VBox vbox = new VBox();
            params = PITCH_OFFSETS;
            comp = new Chooser("Pitch Offset", this, "part" + part + "osc" + osc + "pitchoffset", params);
            vbox.add(comp);

            HBox hbox2 = new HBox();
            params = OSCILLATOR_SOURCES;
            comp = new Chooser("Oscillator Source", this, "part" + part + "osc" + osc + "source", params);
            hbox2.add(comp);

            comp = new CheckBox("Sync", this, "part" + part + "osc" + osc + "sync");
            ((CheckBox)comp).addToWidth(2);
            hbox2.add(comp);
            vbox.add(hbox2);
            hbox.add(vbox);
            }

        VBox vbox = new VBox();
        params = MOD_SOURCES;
        comp = new Chooser("Pitch Mod Source", this, "part" + part + "osc" + osc + "pitchmodsource", params);
        vbox.add(comp);

        params = MOD_SOURCES;
        comp = new Chooser("Pulse Width Mod Source", this, "part" + part + "osc" + osc + "pulsewidthmodsource", params);
        vbox.add(comp);
        hbox.add(vbox);

        hbox.add(Strut.makeHorizontalStrut(8));

        comp = new LabelledDial("Pitch Mod", this, "part" + part + "osc" + osc + "pitchmod", color, 0, 127, 64);
        hbox.add(comp);

        comp = new LabelledDial("Detune", this, "part" + part + "osc" + osc + "detune", color, 0, 127, 64);
        hbox.add(comp);

        comp = new LabelledDial("Pulse Width", this, "part" + part + "osc" + osc + "pulsewidth", color, 0, 127, 64);
        hbox.add(comp);

        comp = new LabelledDial("Pulse Width", this, "part" + part + "osc" + osc + "pulsewidthmod", color, 0, 127, 64);
        ((LabelledDial)comp).addAdditionalLabel("Mod");
        hbox.add(comp);

        comp = new LabelledDial("Waveform", this, "part" + part + "osc" + osc + "waveform", color, 0, 127);
        hbox.add(comp);

        category.add(hbox, BorderLayout.CENTER);
        return category;
        }
        
    public JComponent addFilter(int part, Color color)
        {
        final Category category = new Category(this, "Filter", color);

        JComponent comp;
        String[] params;
        final HBox hbox = new HBox();
        
        VBox vbox = new VBox();

        params = FILTER_TYPES;
        comp = new Chooser("Type", this, "part" + part + "filtertype", params);
        vbox.add(comp);

        comp = new CheckBox("On", this, "part" + part + "filteron");
        vbox.add(comp);
        hbox.add(vbox);
        vbox = new VBox();

        params = KEY_TRACKING;
        comp = new Chooser("Key Tracking", this, "part" + part + "filterkeytracking", params);
        vbox.add(comp);

        params = MOD_SOURCES;
        comp = new Chooser("Resonance Mod Source", this, "part" + part + "filterresmodsource", params);
        vbox.add(comp);
        hbox.add(vbox);

        hbox.add(Strut.makeHorizontalStrut(8));

        comp = new LabelledDial("Frequency", this, "part" + part + "filterfreq", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Resonance", this, "part" + part + "filterres", color, 0, 127);
        hbox.add(comp);

        comp = new LabelledDial("Envelope 2 Mod", this, "part" + part + "filterenvmod", color, 0, 127, 64);
        hbox.add(comp);

        comp = new LabelledDial("LFO 2 Mod", this, "part" + part + "filterlfomod", color, 0, 127, 64);
        hbox.add(comp);

        comp = new LabelledDial("Resonance Mod", this, "part" + part + "filterresmod", color, 0, 127, 64);
        hbox.add(comp);

        category.add(hbox, BorderLayout.CENTER);
        return category;
        }                
 
    int denybble(byte[] data, int pos)
        {
        // The spec is wrong: it's LSB, then MSB
        // int v = (data[pos] << 4) | data[pos+1];
        int v = (data[pos + 1] << 4) | data[pos];
        // Some of the dark star stuff is 8-bit, so we have to make sure we're positive
        if (v < 0) v += 256;
        return v;
        }
                
    public boolean parseVoiceData(byte[] data, int pos, int part)
        {
        part++;         // we're 1...5, not 0...4

        model.set("part" + part + "lfo1speed", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "lfo2speed", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "lfo1delay", denybble(data, pos));
        pos += 2;
        
        // a bug in the DarkStar sysex for factory patches sometimes sends 148.  We are assuming this is actually 0.
        int bug = denybble(data, pos);
        if (bug == 148) bug = 0;
        model.set("part" + part + "lfo2delay", bug);
        pos += 2;
        
        int val = denybble(data, pos);
        model.set("part" + part + "lfo1shape", val & 0x07);
        model.set("part" + part + "lfo1sync", (val >>> 3) & 0x01);
        model.set("part" + part + "lfo1midisync", (val >>> 4) & 0x0F);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "lfo2shape", val & 0x07);
        model.set("part" + part + "lfo2sync", (val >>> 3) & 0x01);
        model.set("part" + part + "lfo2midisync", (val >>> 4) & 0x0F);
        pos += 2;
        model.set("part" + part + "env1attack", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env2attack", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env1decay", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env2decay", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env1sustain", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env2sustain", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env1release", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "env2release", denybble(data, pos));
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "env1velocity", val & 0x07);
        model.set("part" + part + "env2velocity", (val >>> 3) & 0x07);
        pos += 2;
        model.set("part" + part + "osc1pitchmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc2pitchmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc1detune", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc2detune", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc1pulsewidth", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc2pulsewidth", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc1pulsewidthmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc2pulsewidthmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc1waveform", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "osc2waveform", denybble(data, pos));
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "osc1pitchmodsource", val & 0x03);
        model.set("part" + part + "osc1pulsewidthmodsource", (val >>> 2) & 0x03);
        model.set("part" + part + "osc2pitchmodsource", (val >>> 4) & 0x03);
        model.set("part" + part + "osc2pulsewidthmodsource", (val >>> 6) & 0x03);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "osc2pitchoffset", val & 0x07);
        model.set("part" + part + "osc2sync", (val >>> 4) & 0x01);
        model.set("part" + part + "osc2source", (val >>> 5) & 0x07);
        pos += 2;
        model.set("part" + part + "mix", 127 - denybble(data, pos));            // On the DarkStar the 100% osc1 is *127* and 100% osc2 is *0* even though it's far left for osc1.  Go figure.
        pos += 2;
        model.set("part" + part + "ring", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "filterfreq", denybble(data, pos));
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "filterkeytracking", val & 0x07);
        model.set("part" + part + "filteron", (val >>> 3) & 0x01);
        model.set("part" + part + "filtertype", (val >>> 4) & 0x03);
        model.set("part" + part + "filterresmodsource", (val >>> 6) & 0x03);
        pos += 2;
        model.set("part" + part + "filterenvmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "filterlfomod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "filterres", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "filterresmod", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "volume", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "pan", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "tremelo", denybble(data, pos));
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "tremelomodsource", val & 0x01);
        model.set("part" + part + "panmodsource", (val >>> 1) & 0x07);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "channel", val & 0x0F);
        model.set("part" + part + "polyphony", (val >>> 4) & 0x0F);
        pos += 2;
        model.set("part" + part + "notelo", denybble(data, pos));
        pos += 2;
        model.set("part" + part + "notehi", denybble(data, pos));
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "transpose", val & 0x3F);
        model.set("part" + part + "sustain", (val >>> 6) & 0x01);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "portamentotime", val & 0x7F);
        model.set("part" + part + "autoglide", (val >>> 7) & 0x01);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "bendrange", val & 0x0F);
        model.set("part" + part + "portamentotype", (val >>> 4) & 0x0F);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "aftertouchpitch", val & 0x0F);
        model.set("part" + part + "aftertouchfilter", (val >>> 4) & 0x0F);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "modwheelpitch", val & 0x0F);
        model.set("part" + part + "modwheelfilter", (val >>> 4) & 0x0F);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "joystickassign", val & 0x07);
        model.set("part" + part + "outputassign", (val >>> 3) & 0x01);
        model.set("part" + part + "auditiontype", (val >>> 4) & 0x0F);
        pos += 2;
        val = denybble(data, pos);
        model.set("part" + part + "partoutputshift", val & 0x01);
        model.set("part" + part + "envshift", (val >>> 1) & 0x01);
        model.set("part" + part + "envhalfshift", (val >>> 2) & 0x01);
        model.set("part" + part + "oscshift", (val >>> 3) & 0x01);
        model.set("part" + part + "oschalfshift", (val >>> 4) & 0x01);
        model.set("part" + part + "lfoshift", (val >>> 5) & 0x01);
        model.set("part" + part + "lfohalfshift", (val >>> 6) & 0x01);
        model.set("part" + part + "filtershift", (val >>> 7) & 0x01);
        pos += 2;
        // last two bytes are unused
        return true;
        }
 
    public static final int NUM_VOICES = 5;
    public static final int VOICE_DATA_LENGTH = 100;
        
    public int parse(byte[] data, boolean fromFile)
        {
        // DarkStar data comes in the following forms
        // VOICEDATA is 100 bytes
        // Others are 1 byte each
        
        // DarkStar or XP2 Single Voice: 108 bytes
        // F0 00 20 3B 02 01 03 VOICEDATA f7
        
        // DarkStar Single Performance: 512 bytes
        // F0 00 20 3B 02 01 01 VOICEDATA(x5) 00 EDITPART 00 00 f7
        
        // XP2 Single Performance: 516 bytes
        // F0 00 20 3B 02 01 01 VOICEDATA(x5) FX1 FX2 FX3 FX4 FX5 DEPTH RATE EDITPART f7
        
        // DarkStar Bulk Dump Single Performance: 514 bytes
        // F0 00 20 3B 02 01 02 PERFNUM(x2) VOICEDATA(x5) 00 EDITPART 00 00 f7
        
        // XP2 Bulk Dump Single Performance: 518 bytes
        // F0 00 20 3B 02 01 02 PERFNUM(x2) VOICEDATA(x5) FX1 FX2 FX3 FX4 FX5 DEPTH RATE EDITPART f7

        // We're gonna try to recognize all of them

        // NOTE: this data is wrong -- it appears that the XP2 Single Performance is *520* bytes,
        // and the XP2 Bulk Dump Single Performance is *522* bytes
        
        // The globals at the end go: FX1 FX2 FX3 FX4 FX5 [0] ChorusDepth ChorusRate CurrentEditPart [0] [0] [0] [0xF7]


        if (data.length == 108)         // Single Voice
            {
            JComboBox combo = new JComboBox(PARTS);
            int result = showMultiOption(this, 
                new String[] { "Part" }, 
                new JComponent[] { combo },
                new String[] { "Select", "Cancel" }, 0, 
                "Single Part",
                "Data for a single part was received.  Where should it go?");
            if (result == 0)
                {
                if (!parseVoiceData(data, 7, combo.getSelectedIndex()))
                    return PARSE_FAILED;
                }
            else
                return PARSE_CANCELLED;
            }
        else if (data.length == 512)            // XP Single Performance
            {
            setXP2(false);
            for(int i = 0; i < NUM_VOICES; i++)
                if (!parseVoiceData(data, 7 + VOICE_DATA_LENGTH * i, i))
                    return PARSE_FAILED;
            model.set("currenteditpart", data[7 + VOICE_DATA_LENGTH * NUM_VOICES + 1]);
            }
        else if (data.length == 520)            // XP2 Single Performance
            {
            setXP2(true);
            for(int i = 0; i < NUM_VOICES; i++)
                if (!parseVoiceData(data, 7 + VOICE_DATA_LENGTH * i, i))
                    return PARSE_FAILED;
            for(int i = 0; i < NUM_VOICES; i++)
                model.set("part" + (i + 1) + "fxsend", data[7 + VOICE_DATA_LENGTH * NUM_VOICES + i]);
            // then comes a 0
            model.set("chorusdepth", data[7 + VOICE_DATA_LENGTH * NUM_VOICES + 6]);
            model.set("chorusrate", data[7 + VOICE_DATA_LENGTH * NUM_VOICES + 7]);
            model.set("currenteditpart", data[7 + VOICE_DATA_LENGTH * NUM_VOICES + 8]);
            // then comes three 0
            }
        else if (data.length == 514)            // XP Bulk Performance
            {
            setXP2(false);
            model.set("number", (data[8] << 4) + data[7]);
            for(int i = 0; i < NUM_VOICES; i++)
                if (!parseVoiceData(data, 9 + VOICE_DATA_LENGTH * i, i))
                    return PARSE_FAILED;
            model.set("currenteditpart", data[9 + VOICE_DATA_LENGTH * NUM_VOICES + 1]);
            }
        else if (data.length == 522)            // XP2 Bulk Performance
            {
            setXP2(true);
            model.set("number", (data[8] << 4) + data[7]);
            for(int i = 0; i < NUM_VOICES; i++)
                if (!parseVoiceData(data, 9 + VOICE_DATA_LENGTH * i, i))
                    return PARSE_FAILED;
            for(int i = 0; i < NUM_VOICES; i++)
                model.set("part" + (i + 1) + "fxsend", data[9 + VOICE_DATA_LENGTH * NUM_VOICES + i]);
            // then comes a 0
            model.set("chorusdepth", data[9 + VOICE_DATA_LENGTH * NUM_VOICES + 6]);
            model.set("chorusrate", data[9 + VOICE_DATA_LENGTH * NUM_VOICES + 7]);
            model.set("currenteditpart", data[9 + VOICE_DATA_LENGTH * NUM_VOICES + 8]);
            // then comes three 0
            revise();
            }
        revise();
        return PARSE_SUCCEEDED;
        }
 
    void addData(byte[] data, int pos, int val)
        {
        // The spec is wrong: it's LSB, then MSB
        data[pos] = (byte)((val >> 0) & 0x0F);
        data[pos + 1] = (byte)((val >> 4) & 0x0F);
        //data[pos + 1] = (byte)((val >> 0) & 0x0F);
        //data[pos] = (byte)((val >> 4) & 0x0F);
        }
 
    public void emitVoiceData(byte[] data, int pos, int part)
        {
        part++;         // we're 1...5, not 0...4
        
        addData(data, pos, model.get("part" + part + "lfo1speed", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "lfo2speed", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "lfo1delay", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "lfo2delay", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "lfo1shape", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "lfo1sync", 0) & 0x01) << 3)  |
            ((model.get("part" + part + "lfo1midisync", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "lfo2shape", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "lfo2sync", 0) & 0x01) << 3)  |
            ((model.get("part" + part + "lfo2midisync", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env1attack", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env2attack", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env1decay", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env2decay", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env1sustain", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env2sustain", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env1release", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "env2release", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "env1velocity", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "env2velocity", 0) & 0x07) << 3));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc1pitchmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc2pitchmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc1detune", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc2detune", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc1pulsewidth", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc2pulsewidth", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc1pulsewidthmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc2pulsewidthmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc1waveform", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "osc2waveform", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "osc1pitchmodsource", 0) & 0x03) << 0 ) |
            ((model.get("part" + part + "osc1pulsewidthmodsource", 0) & 0x03) << 2)  |
            ((model.get("part" + part + "osc2pitchmodsource", 0) & 0x03) << 4)  |
            ((model.get("part" + part + "osc2pulsewidthmodsource", 0) & 0x03) << 6));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "osc2pitchoffset", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "osc2sync", 0) & 0x01) << 4)  |
            ((model.get("part" + part + "osc2source", 0) & 0x07) << 5));
        pos += 2;
        addData(data, pos, 127 - model.get("part" + part + "mix", 0));          // On the DarkStar the 100% osc1 is *127* and 100% osc2 is *0* even though it's far left for osc1.  Go figure.
        pos += 2;
        addData(data, pos, model.get("part" + part + "ring", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "filterfreq", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "filterkeytracking", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "filteron", 0) & 0x01) << 3)  |
            ((model.get("part" + part + "filtertype", 0) & 0x03) << 4)  |
            ((model.get("part" + part + "filterresmodsource", 0) & 0x03) << 6));
        pos += 2;
        addData(data, pos, model.get("part" + part + "filterenvmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "filterlfomod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "filterres", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "filterresmod", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "volume", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "pan", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "tremelo", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "tremelomodsource", 0) & 0x01) << 0 ) |
            ((model.get("part" + part + "panmodsource", 0) & 0x07) << 1));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "channel", 0) & 0x0F) << 0 ) |
            ((model.get("part" + part + "polyphony", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos, model.get("part" + part + "notelo", 0));
        pos += 2;
        addData(data, pos, model.get("part" + part + "notehi", 0));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "transpose", 0) & 0x3F) << 0 ) |
            ((model.get("part" + part + "sustain", 0) & 0x01) << 6));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "portamentotime", 0) & 0x7F) << 0 ) |
            ((model.get("part" + part + "autoglide", 0) & 0x01) << 7));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "bendrange", 0) & 0x0F) << 0 ) |
            ((model.get("part" + part + "portamentotype", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "aftertouchpitch", 0) & 0x0F) << 0 ) |
            ((model.get("part" + part + "aftertouchfilter", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "modwheelpitch", 0) & 0x0F) << 0 ) |
            ((model.get("part" + part + "modwheelfilter", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "joystickassign", 0) & 0x07) << 0 ) |
            ((model.get("part" + part + "outputassign", 0) & 0x01) << 3)  |
            ((model.get("part" + part + "auditiontype", 0) & 0x0F) << 4));
        pos += 2;
        addData(data, pos,
            ((model.get("part" + part + "partoutputshift", 0) & 0x01) << 0 ) |
            ((model.get("part" + part + "envshift", 0) & 0x01) << 1)  |
            ((model.get("part" + part + "envhalfshift", 0) & 0x01) << 2)  |
            ((model.get("part" + part + "oscshift", 0) & 0x01) << 3)  |
            ((model.get("part" + part + "oschalfshift", 0) & 0x01) << 4)  |
            ((model.get("part" + part + "lfoshift", 0) & 0x01) << 5)  |
            ((model.get("part" + part + "lfohalfshift", 0) & 0x01) << 6)  |
            ((model.get("part" + part + "filtershift", 0) & 0x01) << 7));
        pos += 2;
        // last two bytes are unused
        }

    public byte[] emit(String key) { return new byte[0]; }              // can't emit individual parameters :-(

    public byte[] emit(Model tempModel, boolean toWorkingMemory, boolean toFile)
        {
        if (tempModel == null)
            tempModel = getModel();

        // DarkStar data comes in the following forms
        // VOICEDATA is 100 bytes
        // Others are 1 byte each
        
        // DarkStar or XP2 Single Voice: 108 bytes
        // F0 00 20 3B 02 01 03 VOICEDATA f7
        
        // DarkStar Single Performance: 512 bytes
        // F0 00 20 3B 02 01 01 VOICEDATA(x5) 00 EDITPART 00 00 f7
        
        // XP2 Single Performance: 516 bytes
        // F0 00 20 3B 02 01 01 VOICEDATA(x5) FX1 FX2 FX3 FX4 FX5 CHORUS DEPTH EDITPART f7
        
        // DarkStar Bulk Dump Single Performance: 514 bytes
        // F0 00 20 3B 02 01 02 PERFNUM(x2) VOICEDATA(x5) 00 EDITPART 00 00 f7
        
        // XP2 Bulk Dump Single Performance: 518 bytes
        // F0 00 20 3B 02 01 02 PERFNUM(x2) VOICEDATA(x5) FX1 FX2 FX3 FX4 FX5 CHORUS DEPTH EDITPART f7

        // We emit all but the first one.  Our choice depends on (1) if it's an XP2 or not 
        // and (2) if we're writing or sending
                
        // NOTE: this data is wrong -- it appears that the XP2 Single Performance is *520* bytes,
        // and the XP2 Bulk Dump Single Performance is *522* bytes
        
        // The globals at the end go: FX1 FX2 FX3 FX4 FX5 [0] ChorusDepth ChorusRate CurrentEditPart [0] [0] [0] [0xF7]

        byte[] data = null;
        if (isXP2())
            {
            int offset = 0;
            if (toWorkingMemory || toFile)
                {
                data = new byte[520];
                data[6] = (byte)0x01;
                offset = 7 + VOICE_DATA_LENGTH * NUM_VOICES;
                for(int i = 0; i < NUM_VOICES; i++)
                    emitVoiceData(data, 7 + VOICE_DATA_LENGTH * i, i);
                }
            else
                {
                data = new byte[522];                           
                data[6] = (byte)0x02;
                // PERFNUM
                int number = tempModel.get("number", 0);
                data[7] = (byte)((number >> 0) & 0x0F);
                data[8] = (byte)((number >> 4) & 0x0F);
                offset = 9 + VOICE_DATA_LENGTH * NUM_VOICES;
                for(int i = 0; i < NUM_VOICES; i++)
                    emitVoiceData(data, 9 + VOICE_DATA_LENGTH * i, i);
                }

            // FX1 FX2 FX3 FX4 FX5 [0] CHORUS DEPTH EDITPART [0] [0] [0]
            for(int i = 0; i < NUM_VOICES; i++)
                {
                int val = model.get("part" + (i + 1) + "fxsend", 0);
                data[offset + i] = (byte)val;
                }
            data[offset + 5] = 0;
            int val = model.get("chorusdepth", 0);
            data[offset + 6] = (byte)val;
            val = model.get("chorusrate", 0);
            data[offset + 7] = (byte)val;
            val = model.get("currenteditpart", 0);
            data[offset + 8] = (byte)val;
            data[offset + 9] = 0;
            data[offset + 10] = 0;
            data[offset + 11] = 0;
            }
        else
            {
            if (toWorkingMemory || toFile)
                {
                data = new byte[512];
                data[6] = (byte)0x01;
                for(int i = 0; i < NUM_VOICES; i++)
                    emitVoiceData(data, 7 + VOICE_DATA_LENGTH * i, i);
                data[7 + VOICE_DATA_LENGTH * NUM_VOICES + 1] = (byte)(model.get("currenteditpart", 0));
                }
            else
                {
                data = new byte[514];
                data[6] = (byte)0x02;
                // PERFNUM
                int number = tempModel.get("number", 0);
                data[7] = (byte)((number >> 0) & 0x0F);
                data[8] = (byte)((number >> 4) & 0x0F);
                for(int i = 0; i < NUM_VOICES; i++)
                    emitVoiceData(data, 9 + VOICE_DATA_LENGTH * i, i);
                data[9 + VOICE_DATA_LENGTH * NUM_VOICES + 1] = (byte)(model.get("currenteditpart", 0));
                }
            }
                        
        data[0] = (byte)0xF0;
        data[1] = (byte)0x00;
        data[2] = (byte)0x20;
        data[3] = (byte)0x3B;
        data[4] = (byte)0x02;
        data[5] = (byte)0x01;
        data[data.length - 1] = (byte)0xF7;
        
        return data;
        }

    public static String getSynthName() { return "Red Sound DarkStar"; }

    public String getPatchName(Model model) 
        {
        return "Patch " + getPatchLocationName(model);
        }

    public String getPatchLocationName(Model model)
        {
        int num = model.get("number", 0) + 1;
        if (num < 10) return "0" + num;
        else return "" + num;
        }
    
    public Model getNextPatchLocation(Model model)
        {
        int number = model.get("number");
        
        number++;
        if (number >= 64)
            {
            number = 0;
            }
                
        Model newModel = buildModel();
        newModel.set("number", number);
        return newModel;
        }

    public void changePatch(Model tempModel) 
        {
        tryToSendMIDI(buildPC(getChannelOut(), tempModel.get("number")));
        }

    public boolean testVerify(Synth synth2, 
        String key,
        Object obj1, Object obj2) 
        {
        if (!isXP2())           // The XP doesn't have any of the following
            {
            if (key.equals("part1fxsend")) return true;
            if (key.equals("part2fxsend")) return true;
            if (key.equals("part3fxsend")) return true;
            if (key.equals("part4fxsend")) return true;
            if (key.equals("part5fxsend")) return true;
            if (key.equals("chorusdepth")) return true;
            if (key.equals("chorusrate")) return true;
            }
        return false;
        }


    // This is really only used to swap parts
    public static final String[] BASIC_PARAMETERS = new String[]    
    {
    "lfo2speed",
    "lfo2speed",
    "lfo1delay",
    "lfo2delay",
    "lfo1shape",
    "lfo1sync",
    "lfo1midisync",
    "lfo2shape",
    "lfo2sync",
    "lfo2midisync",
    "env1attack",
    "env2attack",
    "env1decay",
    "env2decay",
    "env1sustain",
    "env2sustain",
    "env1release",
    "env2release",
    "env1velocity",
    "env2velocity",
    "osc1pitchmod",
    "osc2pitchmod",
    "osc1detune",
    "osc2detune",
    "osc1pulsewidth",
    "osc2pulsewidth",
    "osc1pulsewidthmod",
    "osc2pulsewidthmod",
    "osc1waveform",
    "osc2waveform",
    "osc1pitchmodsource",
    "osc1pulsewidthmodsource",
    "osc2pitchmodsource",
    "osc2pulsewidthmodsource",
    "osc2pitchoffset",
    "osc2sync",
    "osc2source",
    "mix",
    "ring",
    "filterfreq",
    "filterkeytracking",
    "filteron",
    "filtertype",
    "filterresmodsource",
    "filterenvmod",
    "filterlfomod",
    "filterres",
    "filterresmod",
    "volume",
    "pan",
    "tremelo",
    "tremelomodsource",
    "panmodsource",
    "channel",
    "polyphony",
    "notelo",
    "notehi",
    "transpose",
    "sustain",
    "portamentotime",
    "autoglide",
    "bendrange",
    "portamentotype",
    "aftertouchpitch",
    "aftertouchfilter",
    "modwheelpitch",
    "modwheelfilter",
    "joystickassign",
    "outputassign",
    "auditiontype",
    "partoutputshift",
    "envshift",
    "envhalfshift",
    "oscshift",
    "oschalfshift",
    "lfoshift",
    "lfohalfshift",
    "filtershift"
    };
    }
