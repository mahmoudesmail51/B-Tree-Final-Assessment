package BTree;

import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException
    {

    }


    
    static void createIndexFile(String filename, int numberOfRecords, int m) throws IOException
    {
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        int counter = 1;
        for (int i = 0; i < numberOfRecords; i++) //rows
        {
            for (int j = 0; j < (m * 2) + 1; j++) // cols
            {
                if (j == 1 && i != numberOfRecords - 1) {
                    file.writeInt(counter);
                    counter++;
                } else {
                    file.writeInt(-1);
                }

            }
            file.writeBytes("\n"); // one byte
        }
        file.close();
    }

    static void displayContentOfFile(String filename) throws IOException
    {
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; j++) {
                int value = 0;
                value = file.readInt();
                System.out.print(value + " ");
            }
            file.seek(file.getFilePointer() + 1);
            System.out.print("\n");

        }
    }

    static void insertRecordAtIndex(String filename, int recordId, int reference) throws IOException
      {
        int rootIndex = 1;
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        ArrayList<Integer> nodesVisited = new ArrayList<>();
        //first node to visit is root
        nodesVisited.add(rootIndex);
        if (isNodeEmpty(filename, rootIndex)) // root
        {
                if (loadFirstFreeNode(filename) == 1)// first empty node = 1 therefore first time to insert in tree
                {
                    file.seek((rootIndex * 36) + (rootIndex) + 4);
                    file.writeInt(recordId);
                    file.writeInt(reference);
                    changeFirstFreeNode(filename, loadFirstFreeNode(filename) + 1);
                    /* change leaf node to 0 */
                    file.seek((rootIndex * 36) + (rootIndex) );
                    file.writeInt(0);
                }
                else
                    { if(isNodeLeaf(filename,rootIndex))
                    {
                        int nextFreePlace = (int) getNextFreeRecord(filename, rootIndex);
                        file.seek(nextFreePlace);
                        file.writeInt(recordId);
                        file.writeInt(reference);
                        int noOfRecordsInNode = getNoOfRecords(filename, rootIndex);
                        sortNode(filename, rootIndex, noOfRecordsInNode);
                    }
                    else // scan for place
                    {
                        ArrayList<Pair<Integer, Integer>> recordsInRoot = loadToArrayList(filename, rootIndex);
                        if (recordId > recordsInRoot.get(recordsInRoot.size() - 1).getKey())
                        {
                            ArrayList<Pair<Integer, Integer>> recordsInNode;
                            nodesVisited.add(recordsInRoot.get(recordsInRoot.size() - 1).getKey());
                            recordsInNode = loadToArrayList(filename,
                                             recordsInRoot.get(recordsInRoot.size() - 1).getKey());
                            if (isNodeEmpty(filename, recordsInRoot.get(recordsInRoot.size() - 1).getKey()))
                            {
                                file.seek(getNextFreeRecord(filename
                                        , recordsInRoot.get(recordsInRoot.size() - 1).getKey()));
                                file.writeInt(recordId);
                                file.writeInt(reference);
                            }
                            else // full
                            { if (loadFirstFreeNode(filename) != -1) // there is a space for another node
                                {
                                    /* splitting */
                                    recordsInNode.add(new Pair<Integer, Integer>(recordId, reference));
                                    int m = 4; // static m = 4
                                    double median = Math.ceil((m + 1) / 2) - 1;
                                    sortArrayOfPairs(recordsInNode);
                                    int medianRecord = recordsInNode.get((int) median).getKey();
                                    int medianReference = recordsInRoot.get(recordsInRoot.size() - 1).getValue();
                                    file.seek(getNextFreeRecord(filename, rootIndex) - 8);
                                    file.writeInt(recordId);
                                    file.writeInt(loadFirstFreeNode(filename));
                                    file.seek(getNextFreeRecord(filename, rootIndex));
                                    file.writeInt(medianRecord);
                                    file.writeInt(medianReference);
                                    sortNode(filename, rootIndex, getNoOfRecords(filename, rootIndex));
                                    clearNode(filename, recordsInRoot.get(recordsInRoot.size() - 1).getValue());
                                    /* left and right elements (smaller than median , larger than median) */
                                    ArrayList<Pair<Integer, Integer>> left = new ArrayList<>();
                                    ArrayList<Pair<Integer, Integer>> right = new ArrayList<>();
                                    for (int i = 0; i < recordsInNode.size(); i++)
                                    {
                                        if (recordsInNode.get(i).getKey() <= medianRecord) {
                                            left.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                    , recordsInNode.get(i).getValue()));
                                            }
                                        else
                                            { right.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                        , recordsInNode.get(i).getValue()));
                                            }
                                    }
                                    /* now write each on its place */
                                    file.seek((medianReference * 36) + (medianReference) + 4);
                                    for (int i = 0; i < left.size(); i++)
                                    {
                                        file.writeInt(left.get(i).getKey());
                                        file.writeInt(left.get(i).getValue());
                                    }
                                    file.seek((loadFirstFreeNode(filename) * 36) +
                                            (loadFirstFreeNode(filename))
                                            + 4);
                                    for (int i = 0; i < right.size(); i++)
                                    {
                                        file.writeInt(right.get(i).getKey());
                                        file.writeInt(right.get(i).getValue());
                                    }
                                }
                            }
                        } else
                            {
                                int place = scanForPlace(filename, recordId);
                                if (isNodeEmpty(filename, place))
                                {
                                    file.seek(getNextFreeRecord(filename, place));
                                    file.writeInt(recordId);
                                    file.writeInt(reference);
                                    sortNode(filename, place, getNoOfRecords(filename, place));
                                }
                                else if (!isNodeEmpty(filename, place))
                                {
                                    double m = 4;
                                    double median = Math.ceil((m + 1) / 2) - 1;
                                    ArrayList<Pair<Integer, Integer>> recordsInNode;
                                    recordsInNode = loadToArrayList(filename, place);
                                    recordsInNode.add(new Pair<Integer, Integer>(recordId, reference));
                                    sortArrayOfPairs(recordsInNode);
                                    int medianRecord = recordsInNode.get((int) median).getKey();
                                    int medianReference = place;
                                    int largestElementRecord = recordsInNode.get(recordsInNode.size() - 1).getKey();
                                    int largestElementReference = loadFirstFreeNode(filename);
                                    /* left and right elements (smaller than median , larger than median) */
                                    ArrayList<Pair<Integer, Integer>> left = new ArrayList<>();
                                    ArrayList<Pair<Integer, Integer>> right = new ArrayList<>();
                                    /* now insert in left and right comparing to median and largest element*/
                                    for (int i = 0; i < recordsInNode.size(); i++)
                                    { if (recordsInNode.get(i).getKey() <= medianRecord)
                                    {
                                            left.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                    , recordsInNode.get(i).getValue()));
                                    }
                                    else {
                                        right.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                , recordsInNode.get(i).getValue()));
                                        }
                                    }
                                    clearNode(filename, medianReference);
                                    /* write each on it place */
                                    file.seek((medianReference * 36) + medianReference + 4);
                                    for (int i = 0; i < left.size(); i++) {
                                        file.writeInt(left.get(i).getKey());
                                        file.writeInt(left.get(i).getValue());
                                    }
                                    file.seek((largestElementReference * 36) + largestElementReference);
                                    file.writeInt(0); // LEAF NO LEAF COUlMN
                                    for (int i = 0; i < right.size(); i++) {
                                        file.writeInt(right.get(i).getKey());
                                        file.writeInt(right.get(i).getValue());
                                    }
                                    /* write median in root */
                                    file.seek(getNextFreeRecord(filename, rootIndex) - 4);
                                    file.writeInt(largestElementReference);
                                    file.writeInt(medianRecord);
                                    file.writeInt(medianReference);
                                    sortNode(filename, rootIndex, getNoOfRecords(filename, rootIndex));
                                    if (loadFirstFreeNode(filename) == 4)
                                    {
                                        changeFirstFreeNode(filename, -1); // no other places
                                    }
                                }
                            }
                    }
                    }
        }
        else//root is full
        { if(isNodeLeaf(filename,rootIndex))
            {
                double m =4;
                /* splitting */
                ArrayList<Pair<Integer,Integer>> recordsInNode = loadToArrayList(filename,rootIndex);
                recordsInNode.add(new Pair<Integer, Integer>(recordId,reference));
                sortArrayOfPairs(recordsInNode);
                /* calculating  median */
                double median =  Math.ceil((m+1)/2)-1; // -1 as array list start from index 0
                /* clearing node to -1 */
               clearNode(filename,rootIndex);
               /* getting median and largest element in the root */
                int medianRecord = recordsInNode.get((int)median).getKey();
                int medianReference= loadFirstFreeNode(filename);
                int largestElementRecord= recordsInNode.get(recordsInNode.size()-1).getKey();
                int largestElementReference=medianReference+1;
                changeFirstFreeNode(filename,largestElementReference+1);
                /* left and right elements (smaller than median , larger than median) */
                ArrayList<Pair<Integer,Integer>> left = new ArrayList<>();
                ArrayList<Pair<Integer,Integer>> right = new ArrayList<>();
                /* now insert in left and right comparing to median and largest element*/
                for(int i=0;i<recordsInNode.size();i++)
                {
                    if(recordsInNode.get(i).getKey()<=medianRecord)
                    {
                     left.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                        ,recordsInNode.get(i).getValue()));
                    }
                    else
                    {
                        right.add(new Pair<Integer, Integer>(recordsInNode.get(i).getKey()
                                                            ,recordsInNode.get(i).getValue()));
                    }
                }
                /* write each array in its place */
                // left
                file.seek((medianReference*36)+(medianReference)+4);
                for(int i=0;i<left.size();i++)
                {
                    file.writeInt(left.get(i).getKey()); file.writeInt(left.get(i).getValue());
                }
                /* write at leaf - no leaf column that is a leaf (value = 0) */
                file.seek((medianReference*36)+medianReference);
                file.writeInt(0);
                //right
                file.seek((largestElementReference*36)+largestElementReference+4);
                for(int i=0;i<right.size();i++)
                {
                    file.writeInt(right.get(i).getKey()); file.writeInt(right.get(i).getValue());
                }
                /* write at leaf - no leaf column that is a leaf (value = 0) */
                file.seek((largestElementReference*36)+largestElementReference);
                file.writeInt(0);
                /* now write new root median and largest element */
                file.seek((rootIndex*36)+(rootIndex)+4);
                file.writeInt(medianRecord);
                file.writeInt(medianReference);
                file.writeInt(largestElementRecord);
                file.writeInt(largestElementReference);
                /* changing leaf column from 0 to 1 */
                file.seek((rootIndex*36)+rootIndex);
                file.writeInt(1);
            }
            else // scan for place
            {
                int place = scanForPlace(filename,recordId);
                if(isNodeEmpty(filename,place))
                {
                    file.seek(getNextFreeRecord(filename,place));
                    file.writeInt(recordId);
                    file.writeInt(reference);
                    int firstRecordOffset=(place*36)+(place*1)+4;
                    int currentRecords = (int) ((file.getFilePointer()-firstRecordOffset)/8);
                    sortNode(filename,place,currentRecords);
                }
                else if(!isNodeEmpty(filename,place))
                {
                    if(loadFirstFreeNode(filename)==-1)
                    {
                        System.out.println("no place to insert");
                    }
                }
            }
        }
    }
    static int searchARecord (String filename,int recordId) throws IOException
    {
        int rootIndex = 1;
        ArrayList<Pair<Integer,Integer>> recordsInNode = loadToArrayList(filename,rootIndex);
        int lastRecord= recordsInNode.size()-1;
        if(recordId>recordsInNode.get(lastRecord).getKey())
        {
            return -1;
        }
        boolean found = false;
        found=isFound(recordsInNode,recordId);
        if(found)
        {
            return 1;
        }
        else // scan
        {
            while(true)
            { int index=0;
                for(int i=0;i<recordsInNode.size();i++)
                {
                     if(recordId<recordsInNode.get(i).getKey())
                     {
                         index = recordsInNode.get(i).getValue();
                         break;
                     }
                }
                recordsInNode = loadToArrayList(filename,index);
                if(isFound(recordsInNode,recordId))
                {
                    return index;
                }
                if(index == 0)
                {
                   return -1;
                }
            }
        }
    }
    static void deleteRecordFromIndex(String filename,int recordId) throws IOException {
        /* first step to search for record */
        int targetNode = searchARecord(filename,recordId);

        ArrayList<Pair<Integer,Integer>> recordsInNode = loadToArrayList(filename,targetNode);

            if(isNodeLeaf(filename,targetNode)) // not a parent
            {
                ArrayList<Pair<Integer,Integer>> newRecords = new ArrayList<>();
                for(int i=0;i<recordsInNode.size();i++)
                {
                    if(recordsInNode.get(i).getKey()!=recordId)
                    {
                        newRecords.add(recordsInNode.get(i));
                    }
                }
                if(newRecords.size()>=2) // greater than minimum
                {
                    clearNode(filename,targetNode);
                    RandomAccessFile file = new RandomAccessFile(filename,"rw");
                    file.seek((targetNode*36)+targetNode+4);
                    for(int i=0;i<newRecords.size();i++)
                    {
                        file.writeInt(newRecords.get(i).getKey()); file.writeInt(newRecords.get(i).getValue());
                    }
                }
                else
                {
                    int parent = getParent(filename,recordId);
                   int LeftSiblingPosition = hasLeftSibling(filename,1,parent);
                    int RightSiblingPosition = hasRightSibling(filename,1,parent);
                    RandomAccessFile file= new RandomAccessFile(filename,"rw");
                    ArrayList<Pair<Integer,Integer>>  leftSiblingRecords = null;
                    ArrayList<Pair<Integer,Integer>>  rightSiblingRecords = null;
                    int leftSiblingReference = 0;
                    int rightSiblingReference = 0;
                    if(LeftSiblingPosition != -1)
                    {
                        file.seek((1*36)+1+4+((LeftSiblingPosition-1)*8)+4); // ro7 le reference bta3 l sibling
                        leftSiblingReference = file.readInt();
                        leftSiblingRecords = loadToArrayList(filename,leftSiblingReference);

                    }
                    if(RightSiblingPosition != -1)
                    {

                        file.seek((1*36)+1+4+((RightSiblingPosition-1)*8)+4); // ro7 le reference bta3 l sibling
                        rightSiblingReference = file.readInt();
                        rightSiblingRecords = loadToArrayList(filename,leftSiblingReference);
                    }
                    //now check on left sibling
                    if(leftSiblingRecords.size() > 0 && leftSiblingRecords.size() > 2)
                    {

                        checkSibling(newRecords,leftSiblingRecords,leftSiblingReference,LeftSiblingPosition,file,filename,targetNode);

                    }
                    else if(rightSiblingRecords.size() > 0 && rightSiblingRecords.size() > 2)
                    {
                        checkSibling(newRecords,rightSiblingRecords,rightSiblingReference,RightSiblingPosition,file,filename,targetNode);
                    }
                    else // merge target node with left sibling or right sibling
                    {
                        ArrayList<Pair<Integer,Integer>> bothNodesRecords = new ArrayList<>();
                        if(LeftSiblingPosition != -1)
                        {
                            // move records from target node to left sibling
                            // target node will be empty you should update parent
                            // add records of target node and left sibling records to bothNodesRecords
                            for(int i=0;i<newRecords.size();i++)
                            {
                                bothNodesRecords.add(newRecords.get(i));
                            }
                            for(int i=0;i<leftSiblingRecords.size();i++)
                            {
                                bothNodesRecords.add(leftSiblingRecords.get(i));
                            }
                            sortArrayOfPairs(bothNodesRecords);
                            clearNode(filename,targetNode);
                            file.seek((targetNode*36)+targetNode);
                            file.writeInt(-1);
                            // now write these records to left sibling
                            file.seek((leftSiblingReference*36)+leftSiblingReference+4);
                            for(int i=0;i<bothNodesRecords.size();i++)
                            {
                                file.writeInt(bothNodesRecords.get(i).getKey());
                                file.writeInt(bothNodesRecords.get(i).getValue());
                            }
                            // update parent
                            ArrayList<Pair<Integer,Integer>> nodesInParent = new ArrayList<>();
                            nodesInParent = loadToArrayList(filename,1);
                            int indexOfLeftSibling = LeftSiblingPosition -1; // 1 --> 0
                            for (int i=0;i<nodesInParent.size();i++)
                            {
                                if(i == indexOfLeftSibling)
                                {

                                    int key = nodesInParent.get(i+1).getKey();
                                    int value = leftSiblingReference;
                                    nodesInParent.remove(i);
                                    nodesInParent.remove(i);
                                    Pair<Integer,Integer> temp = new Pair<Integer, Integer>(key,value);
                                    nodesInParent.add(temp);
                                    sortArrayOfPairs(nodesInParent);
                                    break;
                                }
                            }
                            // go write on parent's place
                            clearNode(filename,1);
                            file.seek((1*36)+1);
                            file.writeInt(1);
                            file.seek((1*36)+1+4);
                            for (int i=0;i<nodesInParent.size();i++) {
                                file.writeInt(nodesInParent.get(i).getKey());
                                file.writeInt(nodesInParent.get(i).getValue());
                            }

                        }
                        else
                        {
                            // move records from target node to right sibling

                            // target node will be empty you should update parent
                            // add records of target node and left sibling records to bothNodesRecords
                            for(int i=0;i<newRecords.size();i++)
                            {
                                bothNodesRecords.add(newRecords.get(i));
                            }
                            for(int i=0;i<rightSiblingRecords.size();i++)
                            {
                                bothNodesRecords.add(rightSiblingRecords.get(i));
                            }
                            sortArrayOfPairs(bothNodesRecords);
                            clearNode(filename,targetNode);
                            file.seek((targetNode*36)+targetNode);
                            file.writeInt(-1);
                            // now write these records to left sibling
                            file.seek((rightSiblingReference*36)+rightSiblingReference+4);
                            for(int i=0;i<bothNodesRecords.size();i++)
                            {
                                file.writeInt(bothNodesRecords.get(i).getKey());
                                file.writeInt(bothNodesRecords.get(i).getValue());
                            }
                            // update parent
                            ArrayList<Pair<Integer,Integer>> nodesInParent = new ArrayList<>();
                            nodesInParent = loadToArrayList(filename,1);
                            int indexOfRightSibling = RightSiblingPosition - 1; // 1 --> 0
                            for (int i=0;i<nodesInParent.size();i++)
                            {
                                if(i == indexOfRightSibling)
                                {
                                    int key = nodesInParent.get(i-1).getKey();
                                    int value = leftSiblingReference;
                                    nodesInParent.remove(i);
                                    nodesInParent.remove(i);
                                    Pair<Integer,Integer> temp = new Pair<Integer, Integer>(key,value);
                                    nodesInParent.add(temp);
                                    sortArrayOfPairs(nodesInParent);
                                    break;
                                }
                            }
                            // go write on parent's place
                            clearNode(filename,1);
                            file.seek((1*36)+1);
                            file.writeInt(1);
                            file.seek((1*36)+1+4);
                            for (int i=0;i<nodesInParent.size();i++) {
                                file.writeInt(nodesInParent.get(i).getKey());
                                file.writeInt(nodesInParent.get(i).getValue());
                            }
                        }
                    }
                }
            }
            else // Node is parent
            {
                ArrayList<Integer> nodeVisited = new ArrayList<>();
                nodeVisited.add(targetNode);
                while(true)
                {
                    int nextTargetNode = 0;
                    for (int i = 0; i < recordsInNode.size(); i++) {
                        if (recordsInNode.get(i).getKey() == recordId) {
                            nextTargetNode = recordsInNode.get(i).getValue();
                            break;
                        }
                    }
                    if (isNodeLeaf(filename, nextTargetNode)) {
                        nodeVisited.add(nextTargetNode);
                        break;
                    } else {
                        recordsInNode = loadToArrayList(filename, nextTargetNode);
                    }
                }

                Collections.reverse(nodeVisited); // start from bottom to top
                RandomAccessFile file = new RandomAccessFile(filename,"rw");
                while ( nodeVisited.size() > 0)
                {
                   int currentIndex = 0;
                   int currentNode = nodeVisited.get(currentIndex);
                   recordsInNode = loadToArrayList(filename,currentNode);
                    ArrayList<Pair<Integer,Integer>> newRecords = new ArrayList<>();
                    for(int i=0;i<recordsInNode.size();i++)
                    {
                        if(recordsInNode.get(i).getKey()!=recordId)
                        {
                            newRecords.add(recordsInNode.get(i));
                        }
                    }
                    if(newRecords.size() >=2 ) // go update parent
                    {
                        clearNode(filename,nodeVisited.get(currentIndex));
                        file.seek((nodeVisited.get(currentIndex)*36)+nodeVisited.get(currentIndex)+4);
                        for(int i=0;i<newRecords.size();i++)
                        {
                            file.writeInt(newRecords.get(i).getKey());
                            file.writeInt(newRecords.get(i).getValue());
                        }
                        nodeVisited.remove(currentIndex);
                       ArrayList<Pair<Integer,Integer>> parentList = loadToArrayList(filename,nodeVisited.get(currentIndex));
                       Pair<Integer,Integer> temp = null;
                       int largestElement = newRecords.get(newRecords.size()-1).getKey();
                       for(int i=0;i<parentList.size();i++)
                       {
                          if(parentList.get(i).getKey() == recordId)
                          {
                              temp = new Pair<Integer, Integer>(largestElement,parentList.get(i).getValue());
                              parentList.remove(i);
                              parentList.add(temp);
                              break;
                          }
                       }
                        sortArrayOfPairs(parentList);
                       file.seek((nodeVisited.get(currentIndex)*36)+nodeVisited.get(currentIndex)+4);
                       for(int i=0;i<parentList.size();i++)
                       {
                           file.writeInt(parentList.get(i).getKey());
                           file.writeInt(parentList.get(i).getValue());
                       }
                       nodeVisited.remove(currentIndex);
                    }
                    else // check on siblings
                    {
                        int parent = getParent(filename,recordId);
                        int leftSiblingPosition = hasLeftSibling(filename,1,parent);
                        int rightSiblingPosition = hasRightSibling(filename,1,parent);
                        file= new RandomAccessFile(filename,"rw");
                        ArrayList<Pair<Integer,Integer>>  leftSiblingRecords = new ArrayList<>();
                        ArrayList<Pair<Integer,Integer>>  rightSiblingRecords = new ArrayList<>();
                        int leftSiblingReference = 0;
                        int rightSiblingReference = 0;
                        if(leftSiblingPosition != -1)
                        {
                            System.out.println("this is left sibling :"+leftSiblingPosition);

                            file.seek((1*36)+1+4+((leftSiblingPosition-1)*8)+4); // ro7 le reference bta3 l sibling
                            leftSiblingReference = file.readInt();
                            leftSiblingRecords = loadToArrayList(filename,leftSiblingReference);


                        }
                        if(rightSiblingPosition != -1)
                        {

                            file.seek((1*36)+1+4+((rightSiblingPosition-1)*8)+4); // ro7 le reference bta3 l sibling
                            rightSiblingReference = file.readInt();
                            rightSiblingRecords = loadToArrayList(filename,rightSiblingReference);
                        }
                        if(leftSiblingRecords.size() > 0 && leftSiblingRecords.size() > 2)
                        {
                            nodeVisited.remove(currentIndex);
                            int largestElement = leftSiblingRecords.size()-1;
                            // borrowed value
                            newRecords.add(leftSiblingRecords.get(largestElement));
                            sortArrayOfPairs(newRecords);
                            int key = leftSiblingRecords.get(largestElement).getKey();
                            leftSiblingRecords.remove(largestElement);
                            clearNode(filename,leftSiblingReference);
                            file.seek((leftSiblingReference*36)+leftSiblingReference+4);
                            for(int i=0;i<leftSiblingRecords.size();i++)
                            {
                                file.writeInt(leftSiblingRecords.get(i).getKey()); file.writeInt(leftSiblingRecords.get(i).getValue());
                            }
                            // update parent node
                            /* update parent */
                            file.seek((1*36)+1+4+((leftSiblingPosition-1)*8));
                            file.writeInt(leftSiblingRecords.get(leftSiblingRecords.size()-1).getKey());
                            file.seek((1*36)+1+4+((leftSiblingPosition-1)*8)+8);
                            file.writeInt(key);
                            nodeVisited.remove(currentIndex);

                        }
                        else if(rightSiblingRecords.size() > 0 && rightSiblingRecords.size() > 2)
                        {
                            nodeVisited.remove(currentIndex);
                            int largestElement = rightSiblingRecords.size()-1;
                            // borrowed value
                            newRecords.add(rightSiblingRecords.get(largestElement));
                            sortArrayOfPairs(newRecords);
                            int key = rightSiblingRecords.get(largestElement).getKey();
                            rightSiblingRecords.remove(largestElement);
                            clearNode(filename, rightSiblingReference);
                            file.seek(( rightSiblingReference*36)+ rightSiblingReference+4);
                            for(int i=0;i<rightSiblingRecords.size();i++)
                            {
                                file.writeInt(rightSiblingRecords.get(i).getKey()); file.writeInt(rightSiblingRecords.get(i).getValue());
                            }
                            // update parent node
                            /* update parent */
                            file.seek((1*36)+1+4+((rightSiblingPosition-1)*8));
                            file.writeInt(leftSiblingRecords.get(rightSiblingRecords.size()-1).getKey());
                            file.seek((1*36)+1+4+((rightSiblingPosition-1)*8)+8);
                            file.writeInt(key);
                            nodeVisited.remove(currentIndex);
                        }
                        else
                        {
                            nodeVisited.remove(currentIndex);
                            ArrayList<Pair<Integer,Integer>> bothNodesRecords = new ArrayList<>();
                            if(leftSiblingPosition != -1)
                            {
                                // move records from target node to left sibling
                                // target node will be empty you should update parent
                                // add records of target node and left sibling records to bothNodesRecords
                                for(int i=0;i<newRecords.size();i++)
                                {
                                    bothNodesRecords.add(newRecords.get(i));
                                }

                                for(int i=0;i<leftSiblingRecords.size();i++)
                                {
                                    bothNodesRecords.add(leftSiblingRecords.get(i));
                                }
                                sortArrayOfPairs(bothNodesRecords);
                                for(int i=0; i < bothNodesRecords.size();i++)
                                    System.out.println(bothNodesRecords.get(i).getKey()+" "+bothNodesRecords.get(i).getValue());
                                clearNode(filename,currentNode);
                                file.seek((currentNode*36)+currentNode);
                                file.writeInt(-1);
                                // now write these records to left sibling
                                file.seek((leftSiblingReference*36)+leftSiblingReference+4);
                                for(int i=0;i<bothNodesRecords.size();i++)
                                {
                                    file.writeInt(bothNodesRecords.get(i).getKey());
                                    file.writeInt(bothNodesRecords.get(i).getValue());
                                }

                                // update parent
                                ArrayList<Pair<Integer,Integer>> nodesInParent = new ArrayList<>();
                                nodesInParent = loadToArrayList(filename,1);
                                System.out.println("this is left sibling position "+leftSiblingPosition);
                                Pair<Integer,Integer> temp;
                                int largestElement = bothNodesRecords.size()-1;
                                int key = bothNodesRecords.get(largestElement).getKey();
                                int nextEmptyNode = 0;
                                for(int i=0;i<nodesInParent.size();i++)
                                {
                                    if(nodesInParent.get(i).getKey() == recordId)
                                    {
                                        nextEmptyNode = nodesInParent.get(i).getValue();
                                        int referenceValueOfLeftSibling = nodesInParent.get(i-1).getValue(); // 1 *2* 3 <--i
                                        nodesInParent.remove(i);
                                        nodesInParent.remove(i-1);
                                        temp = new Pair<Integer, Integer>(key,referenceValueOfLeftSibling);
                                        nodesInParent.add(temp);
                                        break;
                                    }
                                }
                                // write it on the parent
                                clearNode(filename,1);
                                file.seek((1*36)+1);
                                file.writeInt(1);
                                file.seek((1*36)+1+4);
                                for (int i=0;i<nodesInParent.size();i++) {
                                    file.writeInt(nodesInParent.get(i).getKey());
                                    file.writeInt(nodesInParent.get(i).getValue());
                                }

                                nodeVisited.remove(currentIndex);

                            }
                            if(rightSiblingPosition != -1)
                            {
                                // move records from target node to right sibling
                                // target node will be empty you should update parent
                                // add records of target node and right sibling records to bothNodesRecords
                                for(int i=0;i<newRecords.size();i++)
                                {
                                    bothNodesRecords.add(newRecords.get(i));
                                }

                                for(int i=0;i<rightSiblingRecords.size();i++)
                                {
                                    bothNodesRecords.add(rightSiblingRecords.get(i));
                                }
                                sortArrayOfPairs(bothNodesRecords);
                                for(int i=0; i < bothNodesRecords.size();i++)
                                    System.out.println(bothNodesRecords.get(i).getKey()+" "+bothNodesRecords.get(i).getValue());
                                clearNode(filename,currentNode);
                                file.seek((currentNode*36)+currentNode);
                                file.writeInt(-1);
                                // now write these records to right sibling
                                file.seek((rightSiblingReference*36)+rightSiblingReference+4);
                                for(int i=0;i<bothNodesRecords.size();i++)
                                {
                                    file.writeInt(bothNodesRecords.get(i).getKey());
                                    file.writeInt(bothNodesRecords.get(i).getValue());
                                }

                                // update parent
                                ArrayList<Pair<Integer,Integer>> nodesInParent = new ArrayList<>();
                                nodesInParent = loadToArrayList(filename,1);
                                Pair<Integer,Integer> temp;
                                int largestElement = bothNodesRecords.size()-1;
                                int key = bothNodesRecords.get(largestElement).getKey();
                                int nextEmptyNode = 0;
                                for(int i=0;i<nodesInParent.size();i++)
                                {
                                    if(nodesInParent.get(i).getKey() == recordId)
                                    {
                                        nextEmptyNode = nodesInParent.get(i).getValue();
                                        int referenceValueOfRightSibling = nodesInParent.get(i+1).getValue(); // 1 *2* 3 <--i
                                        nodesInParent.remove(i);
                                        nodesInParent.remove(i-1);
                                        temp = new Pair<Integer, Integer>(key,referenceValueOfRightSibling);
                                        nodesInParent.add(temp);
                                        break;
                                    }
                                }
                                // write it on the parent
                                clearNode(filename,1);
                                file.seek((1*36)+1);
                                file.writeInt(1);
                                file.seek((1*36)+1+4);
                                for (int i=0;i<nodesInParent.size();i++) {
                                    file.writeInt(nodesInParent.get(i).getKey());
                                    file.writeInt(nodesInParent.get(i).getValue());
                                }

                                nodeVisited.remove(currentIndex);
                            }

                        }
                    }

                    }
            }
    }

    static int hasRightSibling(String filename,int targetNode,int recordId) throws IOException
    {
        int counter =1;
        ArrayList<Pair<Integer,Integer>> recordsInNode = loadToArrayList(filename,targetNode);
        for(int i=0;i<recordsInNode.size();i++)
        {
            if(recordId==recordsInNode.get(i).getKey())
            {
                break;
            }

            else
                {
                counter++;
            }

        } // 1 *2* 3 4 5
        if(counter < getNoOfRecords(filename,targetNode))
        {
            return counter+1;
        }
        else
            {
                return -1;
            }

    }
    static int hasLeftSibling(String filename,int targetNode,int recordId) throws IOException
    {
       int counter =1;
       ArrayList<Pair<Integer,Integer>> recordsInNode = loadToArrayList(filename,targetNode);
       for(int i=0;i<recordsInNode.size();i++)
       {
           if(recordId==recordsInNode.get(i).getKey())
           {
               break;
           }

           else
           {
               counter++;
           }

       }// 1 2 3 4 5 6 7 8
       if(counter !=1)
       {
           return counter -1;
       }
       else
       {
           return -1;
       }
   }

    static int getParent(String filename,int childRecord) throws IOException
     {
     int rootIndex= 1;
     ArrayList<Pair<Integer,Integer>> recordsInRoot = loadToArrayList(filename,rootIndex);
     int parent =0  ;
     for(int i=0;i<recordsInRoot.size();i++)
     {
         if(childRecord <= recordsInRoot.get(i).getKey())
         {
             parent = recordsInRoot.get(i).getKey();
             return parent;
         }
     }
        return parent;
 }

    static boolean isFound(ArrayList<Pair<Integer,Integer>> list , int recordId)
    {
            boolean found = false;

             for(int i=0;i<list.size();i++)
            {
                 if(recordId == list.get(i).getKey())
                {
                    found = true;

                    break;
                }
            }
        if(found)
        {
         return true;
        }
     return false;
        }
    static void sortNode(String filename,int nodeIndex,long recordsToSort) throws IOException
    {
        RandomAccessFile file = new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex*36)+(nodeIndex)+4);
        TreeMap<Integer,Integer> treeMap = new TreeMap<Integer, Integer>();
        for(int i=0;i<recordsToSort;i++) // read records and sort
        {
            int key=0, value=0;
            key=file.readInt();
            value=file.readInt();
            treeMap.put(key,value);
        }
        file.seek((nodeIndex*36)+(nodeIndex)+4);
        for (Map.Entry<Integer, Integer>
                entry : treeMap.entrySet())
        {
            file.writeInt(entry.getKey());
            file.writeInt(entry.getValue());
        }
        file.close();
    }
    static boolean isNodeEmpty(String filename,int nodeIndex) throws IOException
    {
        RandomAccessFile file = new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex * 36)+(nodeIndex)+32);// plus one for new line byte + 32 to seek to the last record's reference id
        int flag = file.readInt();
        return flag == -1;
    }
    static int loadFirstFreeNode(String filename) throws IOException
    {
        int firstFreeNode=0;
        RandomAccessFile file = new RandomAccessFile(filename,"rw");
        file.seek(4);
        firstFreeNode=file.readInt();
        return firstFreeNode;
    }
    static void changeFirstFreeNode(String filename,int newValue) throws IOException
    {
        RandomAccessFile file=new RandomAccessFile(filename,"rw");
        file.seek(4);
        file.writeInt(newValue);
    }

    static long getNextFreeRecord(String filename, int firstFreeNode) throws IOException
    {
        if(isNodeEmpty(filename,firstFreeNode))
        {
            RandomAccessFile file = new RandomAccessFile(filename,"rw");
            file.seek((firstFreeNode*36)+(firstFreeNode)+4);// plus one for new line 1 byte plus four to ignore the first column (leaf or no leaf)
            int counter=0;
            while(true)
            {

                int flag = file.readInt(); //5 10 -1
                if(flag == -1)
                {
                    return file.getFilePointer() -4;
                }
            }
        }
        else
        {
            return -1;
        }
    }
    static boolean isNodeLeaf(String filename,int nodeIndex) throws IOException
    {
        RandomAccessFile file= new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex*36)+(nodeIndex));
        int flag = file.readInt();
        return flag == 0;
    }
    static int getNoOfRecords(String filename,int nodeIndex) throws IOException
    {
        RandomAccessFile file= new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex*36)+(nodeIndex)+4);
        int temp =0;
        if(getNextFreeRecord(filename,nodeIndex)==-1)
        {
            temp = (nodeIndex*36)+(1*nodeIndex)+4+32;
        }
        else
        {
            temp = (int) getNextFreeRecord(filename,nodeIndex);
        }

        return (int) ((temp-file.getFilePointer())/8);
    }
    static void sortArrayOfPairs(ArrayList<Pair<Integer,Integer>> list)
    {
        Pair <Integer,Integer> temp;

        for (int i=0;i<list.size();i++)
        {
            for (int j=i+1 ; j <list.size(); j++)
            {
                if(list.get(i).getKey()>list.get(j).getKey())
                {
                    temp= new Pair<Integer, Integer>(list.get(i).getKey(),list.get(i).getValue());
                    list.set(i,list.get(j));
                    list.set(j,temp);
                }
            }
        }
    }
    static ArrayList<Pair<Integer,Integer>> loadToArrayList(String filename,int nodeIndex) throws IOException
    {
        ArrayList<Pair<Integer,Integer>> recordsInNode= new ArrayList<>();
        int noOfRecordsInNode = getNoOfRecords(filename,nodeIndex);
        RandomAccessFile file = new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex*36)+(nodeIndex)+4);
        for(int i=0;i<noOfRecordsInNode;i++)
        {
            int key =0, value=0;
            key= file.readInt(); value=file.readInt();
            recordsInNode.add(new Pair<Integer, Integer>(key,value));
        }
        return recordsInNode;
    }
    static void clearNode (String filename,int nodeIndex) throws IOException
    {
        RandomAccessFile file= new RandomAccessFile(filename,"rw");
        file.seek((nodeIndex*36)+(nodeIndex)+4);
        for(int i=0;i<8;i++)
        {
            file.writeInt(-1);
        }
    }

    static int scanForPlace(String filename,int recordId) throws IOException
    {
        int prevIndex = 1;
        RandomAccessFile file = new RandomAccessFile(filename,"rw");
        ArrayList<Pair<Integer,Integer>> recordsInNode;
        while(true)
        {
            file.seek((prevIndex*36)+(prevIndex)+4);
          recordsInNode=loadToArrayList(filename,prevIndex);
            int newIndex = 0;
            for(int i=0;i<recordsInNode.size();i++)
            {
                if(recordId<recordsInNode.get(i).getKey())
                {
                    newIndex=recordsInNode.get(i).getValue();
                    break;
                }
            }

            if(isNodeEmpty(filename,newIndex) && isNodeLeaf(filename,newIndex))

            {
                return newIndex;

            }
            if(isNodeEmpty(filename,newIndex) && !isNodeLeaf(filename,newIndex))
            {

                prevIndex=newIndex;
            }
            if(!isNodeEmpty(filename,newIndex) && isNodeLeaf(filename,newIndex))
            {
                return newIndex;
            }
            if(!isNodeEmpty(filename,newIndex) && !isNodeLeaf(filename,newIndex))
            {

                prevIndex=newIndex;
            }

        }

    }

    static void  checkSibling (ArrayList<Pair<Integer,Integer>> newRecords,ArrayList<Pair<Integer,Integer>> siblingRecords, int siblingReference,int siblingPosition,RandomAccessFile file,String filename,int targetNode) throws IOException {
        int largestElement = siblingRecords.size()-1;
        // borrowed value
        newRecords.add(siblingRecords.get(largestElement));
        sortArrayOfPairs(newRecords);
        siblingRecords.remove(largestElement);
        clearNode(filename,siblingReference);
        file.seek((siblingReference*36)+siblingReference+4);
        for(int i=0;i<siblingRecords.size();i++)
        {
            file.writeInt(siblingRecords.get(i).getKey()); file.writeInt(siblingRecords.get(i).getValue());
        }
        /* update parent */
        file.seek((1*36)+1+4+((siblingPosition-1)*8));
        file.writeInt(siblingRecords.get(siblingRecords.size()-1).getKey());
        /* go write new records with the borrowed value from left sibling */
        file.seek((targetNode*36)+targetNode+4);
        for(int i=0;i<newRecords.size();i++)
        {
            file.writeInt(newRecords.get(i).getKey()); file.writeInt(newRecords.get(i).getValue());
        }

    }


}

