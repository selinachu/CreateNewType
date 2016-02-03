/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.consumer;

import org.apache.ctakes.dictionary.lookup2.concept.Concept;
import org.apache.ctakes.dictionary.lookup2.concept.ConceptCode;
import org.apache.ctakes.dictionary.lookup2.textspan.TextSpan;
import org.apache.ctakes.dictionary.lookup2.util.CuiCodeUtil;
import org.apache.ctakes.dictionary.lookup2.util.SemanticUtil;
import org.apache.ctakes.dictionary.lookup2.util.collection.CollectionMap;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.*;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;

import static org.apache.ctakes.typesystem.type.constants.CONST.*;


/**
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 1/9/14
 */
final public class DefaultTermConsumer extends AbstractTermConsumer {


   public DefaultTermConsumer( final UimaContext uimaContext, final Properties properties ) {
      super( uimaContext, properties );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void consumeTypeIdHits( final JCas jcas, final String defaultScheme, final int cTakesSemantic,
                                  final CollectionMap<TextSpan, Long, ? extends Collection<Long>> textSpanCuis,
                                  final CollectionMap<Long, Concept, ? extends Collection<Concept>> cuiConcepts )
         throws AnalysisEngineProcessException {
      // Collection of UmlsConcept objects
      final Collection<UmlsConcept> umlsConceptList = new ArrayList<>();
      try {
         for ( Map.Entry<TextSpan, ? extends Collection<Long>> spanCuis : textSpanCuis ) {
            umlsConceptList.clear();
            for ( Long cuiCode : spanCuis.getValue() ) {
               umlsConceptList
                     .addAll( createUmlsConcepts( jcas, defaultScheme, cTakesSemantic, cuiCode, cuiConcepts ) );
            }
            final FSArray conceptArr = new FSArray( jcas, umlsConceptList.size() );
            int arrIdx = 0;
            for ( UmlsConcept umlsConcept : umlsConceptList ) {
               conceptArr.set( arrIdx, umlsConcept );
               arrIdx++;
            }
            final IdentifiedAnnotation annotation = createSemanticAnnotation( jcas, cTakesSemantic );
            annotation.setTypeID( cTakesSemantic );
            annotation.setBegin( spanCuis.getKey().getStart() );
            annotation.setEnd( spanCuis.getKey().getEnd() );
            annotation.setDiscoveryTechnique( CONST.NE_DISCOVERY_TECH_DICT_LOOKUP );
            annotation.setOntologyConceptArr( conceptArr );
            annotation.addToIndexes();
         }
      } catch ( CASRuntimeException crtE ) {
         // What is really thrown?  The jcas "throwFeatMissing" is not a great help
         throw new AnalysisEngineProcessException( crtE );
      }
   }

   static private IdentifiedAnnotation createSemanticAnnotation( final JCas jcas, final int cTakesSemantic ) {
      switch ( cTakesSemantic ) {
         case NE_TYPE_ID_DRUG: {
            return new MedicationMention( jcas );
         }
         case NE_TYPE_ID_ANATOMICAL_SITE: {
            return new AnatomicalSiteMention( jcas );
         }
         case NE_TYPE_ID_DISORDER: {
            return new DiseaseDisorderMention( jcas );
         }
         case NE_TYPE_ID_FINDING: {
            return new SignSymptomMention( jcas );
         }
         case NE_TYPE_ID_LAB: {
            return new LabMention( jcas );
         }
         case NE_TYPE_ID_PROCEDURE: {
            return new ProcedureMention( jcas );
         }
         case NE_TYPE_ID_GENE: { //*** CHANGED --- NE_TYPE_ID_GENE: defined in CONST.java 
             return new GeneMention( jcas );
         }
         case NE_TYPE_ID_PRTN: { //*** CHANGED --- NE_TYPE_ID_PRTN: defined in CONST.java 
             return new AminoAcidProteinMention( jcas );
         }
         case NE_TYPE_ID_TISS: { //*** CHANGED --- NE_TYPE_ID_PRTN: defined in CONST.java 
             return new TissueMention( jcas );
         }
      }
      return new EntityMention( jcas );
   }

   static private Collection<UmlsConcept> createUmlsConcepts( final JCas jcas,
                                                              final String defaultScheme,
                                                              final int cTakesSemantic,
                                                              final Long cuiCode,
                                                              final CollectionMap<Long, Concept, ? extends Collection<Concept>> conceptMap ) {
      final Collection<Concept> concepts = conceptMap.getCollection( cuiCode );
      if ( concepts == null || concepts.isEmpty() ) {
         return Arrays.asList( createUmlsConcept( jcas, defaultScheme,
               CuiCodeUtil.getInstance().getAsCui( cuiCode ), null, null, null ) );
      }
      final Collection<UmlsConcept> umlsConcepts = new HashSet<>();
      for ( Concept concept : concepts ) {
         final Collection<String> tuis = concept.getCodes( ConceptCode.TUI );
         if ( !tuis.isEmpty() ) {
            for ( String tui : tuis ) {
               // the concept could have tuis outside this cTakes semantic group
               if ( SemanticUtil.getTuiSemanticGroupId( tui ) == cTakesSemantic ) {
                  umlsConcepts.addAll( createUmlsConcepts( jcas, defaultScheme, tui, concept ) );
               }
            }
         } else {
            umlsConcepts.addAll( createUmlsConcepts( jcas, defaultScheme, null, concept ) );
         }
      }
      return umlsConcepts;
   }

   static private Collection<UmlsConcept> createUmlsConcepts( final JCas jcas, final String defaultScheme,
                                                              final String tui, final Concept concept ) {
      final Collection<UmlsConcept> concepts = new ArrayList<>();
      for ( ConceptCode secondaryScheme : ConceptCode.values() ) {
         if ( secondaryScheme == ConceptCode.TUI ) {
            continue;
         }
         final Collection<String> codes = concept.getCodes( secondaryScheme );
         if ( codes == null || codes.isEmpty() ) {
            continue;
         }
         for ( String code : codes ) {
            concepts.add( createUmlsConcept( jcas, secondaryScheme.name(), concept.getCui(), tui,
                  concept.getPreferredText(), code ) );
         }
      }
      if ( concepts.isEmpty() ) {
         concepts.add( createUmlsConcept( jcas, defaultScheme, concept.getCui(), tui, concept
               .getPreferredText(), null ) );
      }
      return concepts;
   }

   static private UmlsConcept createUmlsConcept( final JCas jcas, final String codingScheme,
                                                 final String cui, final String tui,
                                                 final String preferredText, final String code ) {
      final UmlsConcept umlsConcept = new UmlsConcept( jcas );
      umlsConcept.setCodingScheme( codingScheme );
      umlsConcept.setCui( cui );
      if ( tui != null ) {
         umlsConcept.setTui( tui );
      }
      if ( preferredText != null && !preferredText.isEmpty() ) {
         umlsConcept.setPreferredText( preferredText );
      }
      if ( code != null ) {
         umlsConcept.setCode( code );
      }
      return umlsConcept;
   }

}
