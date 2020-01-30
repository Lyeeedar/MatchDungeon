package com.lyeeedar.Components

enum class ComponentType private constructor(val constructor: ()->AbstractComponent)
{
	// Engine
	ArchetypeBuilder({ ArchetypeBuilderComponent() }),
	AdditionalRenderable({ AdditionalRenderableComponent() }),
	Dialogue({ DialogueComponent() }),
	DirectionalSprite({ DirectionalSpriteComponent() }),
	Event({ EventComponent() }),
	LoadData({ LoadDataComponent() }),
	MarkedForDeletion({ MarkedForDeletionComponent() }),
	MetaRegion({ MetaRegionComponent() }),
	Name({ NameComponent() }),
	Occludes({ OccludesComponent() }),
	Position({ PositionComponent() }),
	Renderable({ RenderableComponent() }),
	Transient({ TransientComponent() }),

	// Game
	AI({ AIComponent() }),
	Container({ ContainerComponent() }),
	Damageable({ DamageableComponent() }),
	EntityArchetype({ EntityArchetypeComponent() }),
	Healable({ HealableComponent() }),
	Matchable({ MatchableComponent() }),
	MonsterEffect({ MonsterEffectComponent() }),
	OnTurn({ OnTurnComponent() }),
	OnTurnEffect({ OnTurnEffectComponent() }),
	OrbSpawner({ OrbSpawnerComponent() }),
	Sinkable({ SinkableComponent() }),
	Special({ SpecialComponent() }),
	Swappable({ SwappableComponent() }),
	Tutorial({ TutorialComponent() });

	companion object
	{
		val Values = ComponentType.values()
		val Temporary = arrayOf( MarkedForDeletion, Transient )
	}
}