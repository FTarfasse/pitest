package org.pitest.mutationtest.execute;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pitest.Description;
import org.pitest.extension.ResultCollector;
import org.pitest.extension.TestUnit;
import org.pitest.functional.F3;
import org.pitest.mutationtest.MutationDetails;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.instrument.TimeOutDecoratedTestSource;
import org.pitest.mutationtest.results.DetectionStatus;


public class MutationTestWorkerTest {

  private MutationTestWorker testee;
  
  @Mock
  private ClassLoader loader;
  
  @Mock
  private Mutater mutater;
  
  @Mock
  private F3<String, ClassLoader,byte[], Boolean> hotswapper;

  @Mock
  private TimeOutDecoratedTestSource testSource;


  @Mock
  private Reporter reporter;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    testee = new MutationTestWorker(hotswapper, mutater, loader);
  }
  
  @Test
  public void shouldDescribeEachExaminedMutation() throws IOException {
    MutationDetails mutantOne = makeMutant("foo",1);
    MutationDetails mutantTwo = makeMutant("foo",2);
    Collection<MutationDetails> range = Arrays.asList(mutantOne, mutantTwo);
    testee.run(range, reporter, testSource);
    verify(reporter).describe(mutantOne.getId());
    verify(reporter).describe(mutantTwo.getId());
  }
 
  
  @Test
  public void shouldReportNoCoverageForMutationWithNoTestCoverage() throws IOException {
    MutationDetails mutantOne = makeMutant("foo",1);
    Collection<MutationDetails> range = Arrays.asList(mutantOne);
    testee.run(range, reporter, testSource);
    verify(reporter).report(mutantOne.getId(),new MutationStatusTestPair(0, DetectionStatus.NO_COVERAGE));
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void shouldReportWhenMutationNotDetected() throws IOException {
    MutationDetails mutantOne = makeMutant("foo",1);
    Collection<MutationDetails> range = Arrays.asList(mutantOne);
    TestUnit tu = makePassingTest();
    when(this.testSource.translateTests(any(List.class))).thenReturn(Collections.singletonList(tu));
    when(this.hotswapper.apply(any(String.class), any(ClassLoader.class), any(byte[].class))).thenReturn(true);
    testee.run(range, reporter, testSource);
    verify(reporter).report(mutantOne.getId(),new MutationStatusTestPair(1, DetectionStatus.SURVIVED));

  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void shouldReportWhenMutationNotViable() throws IOException {
    MutationDetails mutantOne = makeMutant("foo",1);
    Collection<MutationDetails> range = Arrays.asList(mutantOne);
    TestUnit tu = makePassingTest();
    when(this.testSource.translateTests(any(List.class))).thenReturn(Collections.singletonList(tu));
    when(this.hotswapper.apply(any(String.class), any(ClassLoader.class), any(byte[].class))).thenReturn(false);
    testee.run(range, reporter, testSource);
    verify(reporter).report(mutantOne.getId(),new MutationStatusTestPair(0, DetectionStatus.NON_VIABLE));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldReportWhenMutationKilledByTest() throws IOException {
    MutationDetails mutantOne = makeMutant("foo",1);
    Collection<MutationDetails> range = Arrays.asList(mutantOne);
    TestUnit tu = makeFailingTest();
    when(this.testSource.translateTests(any(List.class))).thenReturn(Collections.singletonList(tu));
    when(this.hotswapper.apply(any(String.class), any(ClassLoader.class), any(byte[].class))).thenReturn(true);
    testee.run(range, reporter, testSource);
    verify(reporter).report(mutantOne.getId(),new MutationStatusTestPair(1, DetectionStatus.KILLED, tu.getDescription().getName()));
  }
  
  private TestUnit makeFailingTest() {
    return new TestUnit() {

      public void execute(ClassLoader loader, ResultCollector rc) {
        rc.notifyStart(getDescription());
        rc.notifyEnd(getDescription(), new AssertionFailedError());
      }

      public Description getDescription() {
        return new Description("atest");
      }
      
    };
  }

  private TestUnit makePassingTest() {
    return new TestUnit() {

      public void execute(ClassLoader loader, ResultCollector rc) {
        rc.notifyStart(getDescription());
        rc.notifyEnd(getDescription());
      }

      public Description getDescription() {
        return new Description("atest");
      }
      
    };
  }
  
  
  public MutationDetails makeMutant(final String clazz, final int index) {
    MutationDetails md =  new MutationDetails(new MutationIdentifier(clazz, index, "mutator"),
        "sourceFile", "desc", "method", 42);
    
    when(mutater.getMutation(md.getId())).thenReturn(new Mutant(md, new byte[0]));
    
    return md;
  }
  
}