package space.n4krug.JACKxer.gui;

import space.n4krug.JACKxer.control.ControlParameter;

class EQNode {

    double x;
    double y;

    ControlParameter<Float> freq;
    ControlParameter<Float> gain;
    ControlParameter<Float> q;

    EQNode(ControlParameter<Float> f,
           ControlParameter<Float> g,
           ControlParameter<Float> q) {

        this.freq = f;
        this.gain = g;
        this.q = q;
    }
}