package drones.models;

import akka.dispatch.Futures;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Created by Cedric on 3/9/2015.
 */
public class LazyProperty<T> {
    private Promise<T> promise;
    private T value;

    public LazyProperty(T value){
        this.value = value;
    }

    public LazyProperty(){
        promise = Futures.promise();
    }

    public void setValue(T value){
        this.value = value;
        if(promise != null){
            promise.success(value);
            promise = null;
        }
    }

    public Future<T> getValue(){
        if(value != null){
            return Futures.successful(value);
        } else {
            return promise.future();
        }
    }

    public T getRawValue(){
        // Might return null!
        return value;
    }
}
