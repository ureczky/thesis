package hu.ureczky.celebrations;

public enum TaskType {
    
    POSITION("calc_position"),
    TIME("calc_time"),
    COMPASS("calc_compass");
    
    public String subDir;
    
    TaskType(String subDir) {
        this.subDir = subDir;
    }
    
    public static final String className = "TaskType";
}
