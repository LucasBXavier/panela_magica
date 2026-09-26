package com.panelamagica.panelamagica;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Regras de camadas. Falham o build se alguém acoplar as camadas de forma indevida. */
@AnalyzeClasses(packages = "com.panelamagica.panelamagica", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "com.panelamagica.panelamagica";

    @ArchTest
    static final ArchRule controllersNaoAcessamRepositoriesNemEntidades = noClasses()
            .that().resideInAPackage(BASE + ".controller..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".repository..", BASE + ".domain.entites..");

    @ArchTest
    static final ArchRule repositoriesNaoDependemDeServicesNemControllers = noClasses()
            .that().resideInAPackage(BASE + ".repository..")
            .should().dependOnClassesThat().resideInAnyPackage(BASE + ".service..", BASE + ".controller..");

    @ArchTest
    static final ArchRule entidadesNaoDependemDeCamadasSuperiores = noClasses()
            .that().resideInAPackage(BASE + ".domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + ".service..", BASE + ".controller..", BASE + ".repository..", BASE + ".mapper..", BASE + ".dto..");

    @ArchTest
    static final ArchRule dtosNaoDependemDeServicesRepositoriesNemControllers = noClasses()
            .that().resideInAPackage(BASE + ".dto..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    BASE + ".service..", BASE + ".repository..", BASE + ".controller..");

    @ArchTest
    static final ArchRule servicosNaoDependemDeControllers = noClasses()
            .that().resideInAPackage(BASE + ".service..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".controller..");

    @ArchTest
    static final ArchRule ninguemAlemDoServiceEDoMapperUsaRepositories = noClasses()
            .that().resideInAnyPackage(BASE + ".controller..", BASE + ".dto..", BASE + ".domain..", BASE + ".exception..")
            .should().dependOnClassesThat().resideInAPackage(BASE + ".repository..");

    /** Ativar ao resolver o P2-4 (o ReceitaService ainda devolve ResponseEntity e usa MediaType). */
    @ArchIgnore
    @ArchTest
    static final ArchRule servicosNaoDependemDaCamadaWeb = noClasses()
            .that().resideInAPackage(BASE + ".service..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.http..");
}
