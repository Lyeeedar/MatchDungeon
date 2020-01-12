package com.lyeeedar.Components

import com.badlogic.ashley.core.Entity

fun loadGameComponents(name: String) = null

fun Entity.isBasicOrb(): Boolean
{
	if (!this.hasComponent(MatchableComponent::class.java)) return false

	if (this.hasComponent(SpecialComponent::class.java)) return false

	if (this.matchable()!!.desc.isNamed) return false

	return true
}