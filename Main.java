
class Main {
  public static void main(String[] args) {
    int numofdoctors = Integer.parseInt(args[0]);
    int numofpatients = Integer.parseInt(args[1]);
    if(numofdoctors > 3 || numofdoctors < 1)
    {
      System.out.println("The number of doctors in the clinic is not allowed. Exiting");
      System.exit(0);
    }
    if(numofpatients > 30 || numofpatients < 0)
    {
      System.out.println("The number of patients in the clinic is not allowed. Exiting");
      System.exit(0);
    }
    System.out.println("Run with "+ numofpatients+ " patients, "+ numofdoctors+" nurses, "+ numofdoctors+" doctors\n");        
    CreateClinic clinic = new CreateClinic(numofpatients, numofdoctors);
  }
}

   