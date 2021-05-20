	/**
 	 * Job class to assist in reading data sent from server
     *
	 * This class includes getters for all fields of this class
	 *
	 */
	public class Job{

		private String job;
		private int startTime;
		private int id;
		private int estimatedRunTime;
		private int cpu;
		private int memory;
		private int disk;

		Job(String[] job){
			this.job = job[0];
			this.startTime = Integer.parseInt(job[1]);
			this.id = Integer.parseInt(job[2]);
			this.estimatedRunTime = Integer.parseInt(job[3]);
			this.cpu = Integer.parseInt(job[4]);
			this.memory = Integer.parseInt(job[5]);
			this.disk = Integer.parseInt(job[6]);
		}

		public String getJob(){
			return this.job;
		}

		public int getStartTime(){
			return this.startTime;
		}

		public int getID(){
			return this.id;
		}

		public int getEstimatedRunTime(){
			return this.estimatedRunTime;
		}

		public int getCoreReq(){
			return this.cpu;
		}

		public int getMemoryReq(){
			return this.memory;
		}

		public int getDiscReq(){
			return this.disk;
		}
	}