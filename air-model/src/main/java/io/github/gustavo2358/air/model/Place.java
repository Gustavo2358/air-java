package io.github.gustavo2358.air.model;


/** Typed place forms. Object place obtains its TypeRef from the publication's declaration. */
public sealed interface Place extends Operand permits Places.ObjectPlace, Places.Choice, Places.RegionSlice {}
