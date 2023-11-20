package main.ci_runners;

import main.OntologyManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Stream;

@Component
@Lazy
public class ImportedOntologyAxiomChecker implements CIRunnable {
    /**
     * {@link Logger} of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportedOntologyAxiomChecker.class);

    private final OntologyManager ontologyManager;

    @Autowired
    public ImportedOntologyAxiomChecker(final OntologyManager ontologyManager) {
        this.ontologyManager = ontologyManager;
    }

    @Override
    public void run() {
        for (final OWLOntology ontology : ontologyManager.getOntologyManager().getOntologies()) {
            //System.out.println("Checking ontology: " + ontology.getOntologyID().getOntologyIRI().orElse(null));
            checkOntology(ontology);
        }
    }

    private void checkOntology(final OWLOntology ontology) {
        if (ontology.getImportsDeclarations().isEmpty())
            return;

        // FIXME: classesInSignature does not only return classes declared in the ontology, but also imported classes that are used in axioms
        ontology.classesInSignature().forEach(cls -> checkClassAxioms(ontology, cls));
    }

    private void checkClassAxioms(OWLOntology ontology, OWLClass cls) {
        Set<OWLClassAxiom> classAxioms = ontology.getAxioms(cls);
        /*System.out.println("####################################################");
        System.out.println("Ontology: " + ontology.getOntologyID().getOntologyIRI().orElse(null));
        System.out.println("Class: " + cls);
        System.out.println("Axioms: " + classAxioms);
        System.out.println("--------------------");*/

        for (OWLOntology importedOntology : ontology.getImports()) {
            if (importedOntology.containsClassInSignature(cls.getIRI())) {
                Set<OWLClassAxiom> importedClassAxioms = importedOntology.getAxioms(cls);
                if (!classAxioms.containsAll(importedClassAxioms)) {
                    int numberOfAxiomsAdded = importedClassAxioms.size() - classAxioms.size();
                    IRI importedOntologyIRI = importedOntology.getOntologyID().getOntologyIRI().orElse(null);
                    IRI ontologyIRI = ontology.getOntologyID().getOntologyIRI().orElse(null);
                    LOGGER.error("{} axioms added in imported ontology '{}' for class '{}'. Class is declared in ontology: {}",
                            numberOfAxiomsAdded, importedOntologyIRI, cls, ontologyIRI);

                }
            }
        }
    }
}
