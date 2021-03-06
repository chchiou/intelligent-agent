// RabbitsGrassSimulationModel
package demo;

import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenHistogram;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;


public class RabbitsGrassSimulationModel extends SimModelImpl {
  // Default Values
  private static final int WORLDXSIZE = 50;
  private static final int WORLDYSIZE = 50;
  private static final int GROWTHRATE = 0;
  private static final int AGENT_MIN_LIFESPAN = 50;
  private static final int AGENT_MAX_LIFESPAN = 70;
  private static final int BRITHTHRESHOLD = 80;
  private static final int INITIALNUMBER = 100;

  private int worldXSize = WORLDXSIZE;
  private int worldYSize = WORLDYSIZE;
  private int growthRate = GROWTHRATE;
  private int agentMinLifespan = AGENT_MIN_LIFESPAN;
  private int agentMaxLifespan = AGENT_MAX_LIFESPAN;
  private int brithThreshold =  BRITHTHRESHOLD;
  private int initialNumber = INITIALNUMBER;
  

  private Schedule schedule;

  private RabbitsGrassSimulationSpace cdSpace;

  private ArrayList agentList;

  private DisplaySurface displaySurf;

  private OpenSequenceGraph amountOfGrassInSpace;
  private OpenSequenceGraph amountOfEgent;
  private OpenHistogram agentenergyDistribution;

  class GrassInSpace implements DataSource, Sequence {

    public Object execute() {
      return new Double(getSValue());
    }

    public double getSValue() {
      return (double)cdSpace.getTotalGrass();
    }
  }
  // For recording and display the number of rabbits
  class EgentInSpace implements DataSource, Sequence{
	  public Object execute(){
		  return new Double(getSValue());		  
	  }
	  public double getSValue(){
		  return (double) countLivingAgents();
	  }
  }

  class agentGrass implements BinDataSource{
    public double getBinValue(Object o) {
      RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)o;
      return (double)cda.getEnergy();
    }
  }

  /**
   * Get a String that serves as the name of the model
   * @return the name of the model
   */
  public String getName(){
    return "Carry And Drop";
  }

  /**
   * Tear down any existing pieces of the model and
   * prepare for a new run.
   */
  public void setup(){
    System.out.println("Running setup");
    cdSpace = null;
    agentList = new ArrayList();
    schedule = new Schedule(5);

    // Tear down Displays
    if (displaySurf != null){
      displaySurf.dispose();
    }
    displaySurf = null;

    if (amountOfGrassInSpace != null){
      amountOfGrassInSpace.dispose();
    }
    amountOfGrassInSpace = null;
    
    if (amountOfEgent != null){
    	amountOfEgent.dispose();
    }
    amountOfEgent = null;

    if (agentenergyDistribution != null){
      agentenergyDistribution.dispose();
    }
    agentenergyDistribution = null;

    // Create Displays
    displaySurf = new DisplaySurface(this, "Rabbits Grass Model Window 1");
    amountOfGrassInSpace = new OpenSequenceGraph("Amount Of Grass In Space",this);
    amountOfEgent = new OpenSequenceGraph("Amount of Rabbits", this);
    agentenergyDistribution = new OpenHistogram("Agent energy", 8, 0);

    // Register Displays
    registerDisplaySurface("Rabbits Grass Model Window 1", displaySurf);
    this.registerMediaProducer("Plot", amountOfGrassInSpace);
    this.registerMediaProducer("Plot", amountOfEgent); 
  }

  /**
   * Initialize the model by building the separate elements that make
   * up the model
   */
  public void begin(){
    buildModel();
    buildSchedule();
    buildDisplay();

    displaySurf.display();
    amountOfGrassInSpace.display();
    amountOfEgent.display();
    agentenergyDistribution.display();
  }

  /**
   * Initialize the basic model by creating the space
   * and populating it with grass and agents.
   */
  public void buildModel(){
    System.out.println("Running BuildModel");
    cdSpace = new RabbitsGrassSimulationSpace(worldXSize, worldYSize);
    cdSpace.spreadGrass(growthRate);

    for(int i = 0; i < initialNumber; i++){
      addNewAgent();
    }
    
    for(int i = 0; i < agentList.size(); i++){
      RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
      cda.report();
    }
  }

  /**
   * Create the schedule object(s) that will be executed
   * during the running of the model
   */
  public void buildSchedule(){
    System.out.println("Running BuildSchedule");

    class CarryDropStep extends BasicAction {
      public void execute() {
        SimUtilities.shuffle(agentList);
        for(int i =0; i < agentList.size(); i++){
          RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
          cda.step();
        }

        int deadAgents = reapDeadAgents();
//        for(int i =0; i < deadAgents; i++){
//          addNewAgent();
//        }
        
        reproduceAgent();
        displaySurf.updateDisplay();       
        }
    }

    schedule.scheduleActionBeginning(0, new CarryDropStep());

    class CarryDropCountLiving extends BasicAction {
      public void execute(){
        countLivingAgents();
      }
    }

    schedule.scheduleActionAtInterval(10, new CarryDropCountLiving());

    class CarryDropUpdateGrassInSpace extends BasicAction {
      public void execute(){
        amountOfGrassInSpace.step();
      }
    }

    schedule.scheduleActionAtInterval(10, new CarryDropUpdateGrassInSpace());

    class CarryDropUpdateAgentenergy extends BasicAction {
      public void execute(){
        agentenergyDistribution.step();
        amountOfEgent.step(); 
      }
    }

    schedule.scheduleActionAtInterval(10, new CarryDropUpdateAgentenergy());
    
    class SimulationSpreadGrass extends BasicAction{
    	public void execute(){
    		cdSpace.spreadGrass(growthRate);
    		displaySurf.updateDisplay();  
    	}
    }
    schedule.scheduleActionAtInterval(10, new SimulationSpreadGrass());
  }

  /**
   * Build the display elements for this model.
   */
  public void buildDisplay(){
    System.out.println("Running BuildDisplay");

    ColorMap map = new ColorMap();

    for(int i = 1; i<16; i++){
      //map.mapColor(i, new Color((int)(i * 8 + 127), 0, 0));
    	map.mapColor(i, 0,1,0);
    }
    map.mapColor(0, Color.black);

    Value2DDisplay displayGrass = 
        new Value2DDisplay(cdSpace.getCurrentGrassSpace(), map);

    Object2DDisplay displayAgents = new Object2DDisplay(cdSpace.getCurrentAgentSpace());
    displayAgents.setObjectList(agentList);

    displaySurf.addDisplayableProbeable(displayGrass, "Grass");
    displaySurf.addDisplayableProbeable(displayAgents, "Agents");

    amountOfGrassInSpace.addSequence("Grass In Space", new GrassInSpace());
    amountOfEgent.addSequence("Agent in Space", new EgentInSpace());
    agentenergyDistribution.createHistogramItem("Agent energy",agentList,new agentGrass());

  }

  /**
   * Add a new agent to this model's agent list and agent space
   */
  private void addNewAgent(){
    RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinLifespan, agentMaxLifespan);
    agentList.add(a);
    cdSpace.addAgent(a);
  }
  
  // reproduce when a rabbit has enough energy and then remove some energy
  private void reproduceAgent(){
	   for(int i = (agentList.size() - 1); i >= 0; i--){
	    RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
//	    if ((cda.getEnergy() > brithThreshold) && (cda.getReproduceNumber() != 1)){
	    if (cda.getEnergy() > brithThreshold) {
	    	addNewAgent();
	    	cda.setEnergy(cda.getEnergy() - 60);
//	    	cda.setReproduceNumber(1);
	    }
	   }
  }

  /**
   * Collect any dead agents from the simulation and distribute
   * their Grass around.
   * @return a count of the agents that died
   */
  private int reapDeadAgents(){
    int count = 0;
    for(int i = (agentList.size() - 1); i >= 0 ; i--){
      RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
      if(cda.getEnergy() < 1){
        cdSpace.removeAgentAt(cda.getX(), cda.getY());
//        cdSpace.spreadGrass(cda.getGrass());
        agentList.remove(i);
        count++;
      }
    }
    return count;
  }

  /**
   * Get a count of the living agents on the model's agent list.
   * @return count of the living agents on the agent list
   */
//  private int countLivingAgents(){
//    int livingAgents = 0;
//    for(int i = 0; i < agentList.size(); i++){
//      RabbitsGrassSimulationAgent cda = (RabbitsGrassSimulationAgent)agentList.get(i);
//      if(cda.getEnergy() > 0) livingAgents++;
//    }
//    System.out.println("Number of living Rabbits is: " + livingAgents);
//    return livingAgents;
//  }
  /**
   * Get a count of the living agents on the model's agent list.
   * @return count of the living agents on the agent list
   */
  private int countLivingAgents(){
	    int livingAgents = agentList.size();
	    System.out.println("Number of living Rabbits is: " + livingAgents);
	    return livingAgents;
	  }

  /**
   * Returns the Schedule object for this model; for use
   * internally by RePast
   * @return the Schedule object for this model
   */
  public Schedule getSchedule(){
    return schedule;
  }

  /**
   * Get the string array that lists the initialization parameters
   * for this model
   * @return a String array that includes the names of all variables
   * that can be modified by the RePast user interface
   */
  public String[] getInitParam(){
    String[] initParams = { "InitialNumber", "WorldXSize", "WorldYSize", "GrowthRate", "BirthThreshold"};
    return initParams;
  }
  /**
   * Get the parameter indicating the number of agents in this model
   * @return the number of agents in the model
   */
  public int getInitialNumber(){
    return initialNumber;
  }

  /**
   * Set the parameter indicating the initial number of agents for this
   * model.
   * @param na new value for initial number of agents.
   */
  public void setInitialNumber(int na){
	  initialNumber = na;
  }

  /**
   * Get the width of the space in the model
   * @return the width of the space object in the model (X-dimension)
   */
  public int getWorldXSize(){
    return worldXSize;
  }

  /**
   * Set the parameter initializing the width of the space
   * object in this model
   * @param wxs the new size of the X-dimension of the model.
   */
  public void setWorldXSize(int wxs){
    worldXSize = wxs;
  }

  /**
   * Get the heighth of the space in the model
   * @return the heighth of the space object in the model (Y-dimension)
   */
  public int getWorldYSize(){
    return worldYSize;
  }
  
  /**
   * Set the parameter initializing the heighth of the space
   * object in this model
   * @param wys the new size of the Y-dimension of the model.
   */
  public void setWorldYSize(int wys){
    worldYSize = wys;
  }

  /**
   * Get the value of the parameter initializing the total amount
   * of Grass in this model
   * @return the initial value for the total amount of Grass in the
   * model
   */
  
  public int getGrowthRate(){
	 return growthRate;
  }
  
  public void  setGrowthRate(int rate){
	 growthRate = rate;
  }
  
  public int getBirthThreshold() {
    return brithThreshold;
  }

  /**
   * Set the new value for the total amount of Grass to be used when
   * initializing the simulation
   * @param i the new value for the total amount of Grass
   */
  public void setBirthThreshold(int i) {
	  brithThreshold = i;
  }

  /**
   * Get the maximum value for an agent's lifespan
   * @return the maximum value for an agent's lifespan
   */
  public int getAgentMaxLifespan() {
    return agentMaxLifespan;
  }

  /**
   * Get the minimum value for an agent's lifespan
   * @return the minimum value for an agent's lifespan
   */
  public int getAgentMinLifespan() {
    return agentMinLifespan;
  }

  /**
   * Set the maximum value for an agent's lifespan
   * @param i the maximum value for an agent's lifespan
   */
  public void setAgentMaxLifespan(int i) {
    agentMaxLifespan = i;
  }

  /**
   * Set the minimum value for an agent's lifespan
   * @param i the minimum value for an agent's lifespan
   */
  public void setAgentMinLifespan(int i) {
    agentMinLifespan = i;
  }

  /**
   * Main method for this model object; this runs the model.
   * @param args Any string arguments to be passed to this model (currently none)
   */
  public static void main(String[] args) {
    SimInit init = new SimInit();
    RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
    init.loadModel(model, "", false);
  }
}
