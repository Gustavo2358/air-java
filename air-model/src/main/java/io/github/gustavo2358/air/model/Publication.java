package io.github.gustavo2358.air.model;


/**
 * Immutable transport-independent AIR publication. Construct first, then validate cross-references.
 * Library version is independent of airVersion. Never combine local IDs across publications.
 */
public record Publication(Ids.PublicationId id, SemanticVersion airVersion,
                          Capabilities.Manifest capabilities,
                          java.util.List<Origins.Artifact> artifacts,
                          java.util.List<Unit> units,
                          java.util.List<Memory.Storage> storage,
                          java.util.List<Interactions.Resource> resources,
                          java.util.List<Artifacts.Relation> artifactRelations,
                          java.util.List<Origins.Origin> origins,
                          Evidence.Coverage coverage,
                          java.util.List<Evidence.Uncertainty> uncertainties,
                          java.util.List<Proofs.Premise> premises) {
    public Publication {
        java.util.Objects.requireNonNull(id,"id"); java.util.Objects.requireNonNull(airVersion,"airVersion");
        java.util.Objects.requireNonNull(capabilities,"capabilities");
        artifacts=java.util.List.copyOf(artifacts); units=java.util.List.copyOf(units);
        storage=java.util.List.copyOf(storage); resources=java.util.List.copyOf(resources);
        artifactRelations=java.util.List.copyOf(artifactRelations); origins=java.util.List.copyOf(origins);
        java.util.Objects.requireNonNull(coverage,"coverage");
        uncertainties=java.util.List.copyOf(uncertainties); premises=java.util.List.copyOf(premises);
    }
}
