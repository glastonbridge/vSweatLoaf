s = Server.local;

s.waitForBoot({

    SynthDef("shiftie",{ 
        arg out = 0, stretch,pitch,trigger = 0;
        var bufp,shif,rshift,rpitch;
        // balance pitch and stretch out against each other
        rpitch = pitch/stretch;
        rshift = stretch;
        bufp = PlayBuf.ar(1, 10, BufRateScale.kr(10)*rpitch,trigger);
        shif = PitchShift.ar(bufp,0.1,rshift,0,0);
        Out.ar(0,shif);
    }).load(s);

})
