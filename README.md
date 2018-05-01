Simple Retrogen [![Build Status](http://jenkins.covers1624.net/job/Alex/job/EasyRetrogen/badge/icon)](http://jenkins.covers1624.net/job/Alex/job/EasyRetrogen/)
===============

This is a simple retrogeneration mod that should enable other mods to retrogenerate their resources.

You should be able to add the mod's IWorldGenerator to the config, and it'll then fire that worldgenerator
at the chunk, potentially retrogenerating the mod's chunk data.

Note: it requires a simple IWorldGenerator, likely only using stuff like WorldGenMineable, to work.

Untested.
