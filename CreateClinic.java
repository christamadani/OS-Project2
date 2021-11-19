
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
//semaphorename.acquire = wait
//semaphore.release = signal


public class CreateClinic {
  int numofpatients;
  int numofdoctors;
  int numofnurses;
  ArrayList<Patient> waitingforreception = new ArrayList<>(); //enteredList
  ArrayList<Patient> waitingfornurse = new ArrayList<>(); //waitroomList
  ArrayList<Patient> waitingfordoctor = new ArrayList<>();
  
  Receptionist receptionist;
  static Semaphore receptionistavailiable = new Semaphore(1); //at first the receptionist is available
  static Semaphore isregistered = new Semaphore(0); //the patient is not registered in the beginning
  static Semaphore waitingandregistered = new Semaphore(0); 
  static Semaphore patientarrived = new Semaphore(0); //entered building semaphore
  static Semaphore Examrooms;
  static Semaphore readyforDoctor = new Semaphore(0); //semaphore for when a patient is ready for a doctor
 // static Semaphore withDoctor = new Semaphore(0);
  
  static Semaphore enteredroom = new Semaphore(0);

  // constructor
  CreateClinic(int patientsn, int doctorsn) {
    numofdoctors = doctorsn;
    numofpatients = patientsn;
    numofnurses = doctorsn;
    receptionist = new Receptionist(); //create receptionist
    Examrooms = new Semaphore(numofdoctors);
    
    for(int i = 0; i<numofdoctors; i++)
    {
      new Doctor(i);  //create doctors 
    }
    for(int i = 0; i<numofpatients; i++)
    {
      try{
        Thread.sleep(10);
        new Patient(i); //create patients 
        }catch(InterruptedException e) {
          System.out.println("InterruptedException caught");
            }
    }
  }
  
  
//PATIENT SUBCLASS

  class Patient implements Runnable {

    int PatientID;
    int doctorSeen;
    int nurseSeen;
    boolean needreceptionist;
    boolean exited;
    Semaphore seenReceptionistSem = new Semaphore(0); //initially 0
    Semaphore seenDoctorSem = new Semaphore(0); //initially 0
    Semaphore mutex1 = new Semaphore(1);
    Semaphore mutex2 = new Semaphore(1);
    int count = 0;

    //constructor with argument being the patient number id 
    Patient(int number) {
      PatientID = number;
      needreceptionist = true;
      exited = false;
      Thread mythread = new Thread(this, "Doctor");
      mythread.start(); //starts 
    }

    @Override
    public void run() { //the thread function 

      System.out.println("Patient " + PatientID + " enters waiting room, waits for receptionist");
      try{
      mutex1.acquire();
      waitingforreception.add(this); //add this patient to the arraylist that is waiting for the receptionist. FIFO
      //signal that the patient has walked into the clinic. AKA waiting for reception or walking towards it. 
      patientarrived.release(); 
      mutex1.release();
      } catch(InterruptedException e){
        System.out.println("InterruptedException caught");
      }

      while (needreceptionist) {
        try {
          receptionistavailiable.acquire(); //wait for receptionist to be ready for the next patient
          needreceptionist = false;

        } catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
      }
      isregistered.release(); //signal that this patient has been registered and the receptionist is ready 
      try {
      mutex2.acquire();
      waitingfornurse.add(this); //add to the queue 
      waitingandregistered.release(); //increment one to the people waiting in the waiting room for the nurse
      mutex2.release();
      }catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
      
      while (!exited) {
        try {
          enteredroom.acquire(); //wait to enter the room
         // withDoctor.acquire();
         
          seenDoctorSem.acquire();
          // wait then print the prompt
          
          System.out.println("Patient " + PatientID + " receives advice from doctor " + doctorSeen);

          // allow the patient to leave
          //Thread.sleep(10);
          System.out.println("Patient " + PatientID + " leaves");
          Thread.sleep(10);
   
          count++;
         
          exited = true; //break out of loop and check to see if last patient

        } catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
      }
     // System.out.println("Patient Finished running");
      if(count==numofpatients)
      {
       // System.out.println("Last Patient left");
        System.exit(0);
      }
    }
  }

//DOCTOR SUBCLASS

 
  class Doctor implements Runnable {
    int DoctorID;
    Nurse nurse;
    int i;
    Patient currentPatient;

    Semaphore doctorSem = new Semaphore(numofdoctors);
    Semaphore mutex3 = new Semaphore(1);

    Doctor(int i) {
      this.DoctorID = i;
      nurse = new Nurse(DoctorID);
      Thread thread = new Thread(this, "Doctor");
      thread.setDaemon(true);
      thread.start();
      
    }

    @Override
    public void run() {
      while (true) {
        try {
          readyforDoctor.acquire(); //waits for when patient is ready and in the exam room 
          doctorSem.acquire(); //decrement doctor
         
          mutex3.acquire();
          currentPatient = waitingfordoctor.remove(0);
          currentPatient.doctorSeen = DoctorID;
          mutex3.release();
          System.out.println("Patient "+currentPatient.PatientID+" enters doctor "+DoctorID+"'s office");
          //withDoctor.release();
          System.out.println("Doctor " + DoctorID + " listens to symptoms from patient "+ currentPatient.PatientID);
          Thread.sleep(10);

          
          currentPatient.seenDoctorSem.release(); //signal that the doctor has seen the patient

          doctorSem.release(); //doctor is not busy now

          Examrooms.release(); //Exam room is incremented

        } catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
       // System.out.println("Doctor Finished running");
      }
    }
  }

//NURSE SUBCLASS


  class Nurse implements Runnable {
    int NurseID;

    /* Semaphore to note that nurse is/isnt busy */
    Semaphore nurseSem = new Semaphore(numofnurses);
    Semaphore mutex1 = new Semaphore(1);
    Semaphore mutex2 = new Semaphore(1);

    Patient currentPatient;

    Nurse(int i) { //created inside the doctor constructor using the same ID
      this.NurseID = i;
      Thread mynursethread = new Thread(this, "Nurse");
      mynursethread.setDaemon(true);
      mynursethread.start();
    }

    @Override
    public void run() {
      while (true) {
        try {
          nurseSem.acquire(); //decrements
          waitingandregistered.acquire(); //wait for a patient that has seen the receptionist
          //currentPatient = waitingfornurse.get(0); //get next patient in the "queue"
          
          //currentPatient.seenReceptionistSem.acquire();
          mutex1.acquire();
          currentPatient = waitingfornurse.remove(0);  //gets the patient from waiting room.
          currentPatient.nurseSeen = NurseID;
          mutex1.release();
          //walkingtoexamroom()
          Examrooms.acquire(); //decrements the number of exam rooms
          // wait then print the prompt
          
          System.out.println("Nurse " + NurseID + " takes patient " + currentPatient.PatientID + " to doctor's office");
          Thread.sleep(10);

          enteredroom.release(); //show that the patient has entered the room
         // produceExamRoom.acquire(); //decrements
          mutex2.acquire();
          waitingfordoctor.add(currentPatient);
          readyforDoctor.release(); //signal that there is need of doctor
          mutex2.release();
          nurseSem.release(); //nurse is released. Assumed that nurse is not busy while the patient is waiting for the doctor


        } catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
      //  System.out.println("Nurse Finished running");
      }
    }
  }

//RECEPTIONIST SUBCLASS

  class Receptionist implements Runnable {
    /* Semaphore to note that doctor is/isnt busy */
    Semaphore receptionistSem = new Semaphore(1);
    Semaphore mutex1 = new Semaphore(1);
    Patient currentPatient;

    Receptionist() {
      Thread thread = new Thread(this, "Receptionist");
      thread.setDaemon(true);
      thread.start();
    }

    @Override
    public void run() {
      while (true) {
        try {
          isregistered.acquire(); //wait
          patientarrived.acquire(); //waits for patient to come in the door
          mutex1.acquire();
          currentPatient = waitingforreception.remove(0); // grab the patient we are checking in
          mutex1.release();

          //Thread.sleep(10);
          System.out.println("Receptionist registers patient " + currentPatient.PatientID);
          Thread.sleep(10);
          currentPatient.needreceptionist = false;
          currentPatient.seenReceptionistSem.release(); // notify the nurse that this one has seen receptionist
          receptionistavailiable.release(); //receptionist can take the next patient
        } catch (InterruptedException e) {
          System.out.println("InterruptedException caught");
        }
        
      }
      
    }
  }

}
